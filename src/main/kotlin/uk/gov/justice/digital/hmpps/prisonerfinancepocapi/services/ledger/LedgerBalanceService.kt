package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionEntryRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.EstablishmentBalance
import java.math.BigDecimal
import java.time.Instant

@Service
class LedgerBalanceService(
  private val transactionRepository: TransactionRepository,
  private val transactionEntryRepository: TransactionEntryRepository,
  private val prisonRepository: PrisonRepository,
) {

  private val prisonerSubAccountCodes = listOf(2101, 2102, 2103)
  private val holdTransactionTypes = listOf("HOA", "HOR", "OHB")
  private val migrationTypes = listOf("OB", "OHB")

  /**
   * Calculates both the total balance and hold balances for a prisoner account
   * by aggregating the per-establishment balances.
   */
  fun calculatePrisonerAccountBalances(account: Account): Pair<BigDecimal, BigDecimal> {
    val establishmentBalances = calculatePrisonerBalancesByEstablishment(account)

    if (establishmentBalances.isEmpty()) {
      return Pair(BigDecimal.ZERO, BigDecimal.ZERO)
    }

    val aggregatedTotalBalance = establishmentBalances.sumOf { it.totalBalance }
    val aggregatedHoldBalance = establishmentBalances.sumOf { it.holdBalance }

    return Pair(aggregatedTotalBalance, aggregatedHoldBalance)
  }

  fun calculateGeneralLedgerAccountBalance(account: Account): BigDecimal {
    if (account.accountCode in prisonerSubAccountCodes) {
      return calculateAggregatedPrisonerGeneralLedgerAccountBalance(account)
    }

    val transactionEntries = getTransactionsSinceLatestBalance(account.id!!)

    if (transactionEntries.isEmpty()) {
      return BigDecimal.ZERO
    }

    return transactionEntries.sumOf { entry ->
      when (entry.entryType) {
        PostingType.DR -> if (account.postingType == PostingType.DR) entry.amount else entry.amount.negate()
        PostingType.CR -> if (account.postingType == PostingType.CR) entry.amount else entry.amount.negate()
      }
    }
  }

  /**
   * Retrieves transaction entries for an account, filtered to exclude transactions
   * that occurred before the latest 'OB' or 'OHB' transaction
   */
  fun getTransactionsSinceLatestBalance(accountId: Long): List<TransactionEntry> {
    val latestMigrationTimestamp = findCutOffTimestamp(accountId)
    val allEntries = transactionEntryRepository.findByAccountId(accountId)

    if (latestMigrationTimestamp == null) {
      return allEntries
    }

    return allEntries.filter { entry ->
      val transaction = transactionRepository.findById(entry.transactionId).orElse(null) ?: return@filter false

      val transactionInstant = transaction.date.toInstant()

      val isMigrationTransaction = transactionInstant.equals(latestMigrationTimestamp) &&
        (transaction.transactionType in migrationTypes)

      val isPostMigration = transactionInstant.isAfter(latestMigrationTimestamp)

      isMigrationTransaction || isPostMigration
    }
  }

  /**
   * Finds the timestamp of the latest Opening Balance ('OB') or Hold Opening Balance ('OHB') transaction
   * for a given account.
   */
  private fun findCutOffTimestamp(accountId: Long): Instant? {
    val transactionIds = transactionEntryRepository.findByAccountId(accountId)
      .map { it.transactionId }
      .distinct()

    return transactionRepository.findAllById(transactionIds)
      .filter { it.transactionType in migrationTypes }
      .maxOfOrNull { it.date.toInstant() }
  }

  /**
   * Handles aggregation for prisoner general ledger accounts (2101, 2102, 2103) by summing the net
   * balance of all transactions for that prison/code combination.
   */
  private fun calculateAggregatedPrisonerGeneralLedgerAccountBalance(account: Account): BigDecimal {
    val targetAccountCode = account.accountCode
    val prison = prisonRepository.findById(account.prisonId!!).orElse(null)?.code

    val aggregatedBalance = transactionEntryRepository.calculateAggregatedBalance(
      accountCode = targetAccountCode,
      prison = prison!!,
    )

    return aggregatedBalance ?: BigDecimal.ZERO
  }

  /**
   * Calculates the total and hold balances for a prisoner's account, broken down by establishment
   * where transactions have occurred.
   */
  fun calculatePrisonerBalancesByEstablishment(account: Account): List<EstablishmentBalance> {
    val allEntries = transactionEntryRepository.findByAccountId(account.id!!)
    if (allEntries.isEmpty()) {
      return emptyList()
    }

    val transactionIds = allEntries.map { it.transactionId }.distinct()
    val allTransactions = transactionRepository.findAllById(transactionIds).associateBy { it.id!! }

    val entriesByPrison: Map<String, List<TransactionEntry>> = allEntries
      .mapNotNull { entry ->
        val transaction = allTransactions[entry.transactionId]
        if (transaction?.prison != null) {
          entry to transaction.prison
        } else {
          null
        }
      }
      .groupBy { it.second }
      .mapValues { it.value.map { (entry, _) -> entry } }

    val results = mutableListOf<EstablishmentBalance>()

    for ((prisonId, entries) in entriesByPrison) {
      val latestMigrationTimestamp = findLatestMigrationTimestampForPrison(account.id, prisonId, allTransactions)

      val postMigrationEntries = entries.filter { entry ->
        val transaction = allTransactions[entry.transactionId] ?: return@filter false
        val transactionInstant = transaction.date.toInstant()

        if (latestMigrationTimestamp == null) {
          true
        } else {
          val isMigrationTransaction = transactionInstant.equals(latestMigrationTimestamp) &&
            (transaction.transactionType in migrationTypes)

          val isPostMigration = transactionInstant.isAfter(latestMigrationTimestamp)

          isMigrationTransaction || isPostMigration
        }
      }

      if (postMigrationEntries.isNotEmpty()) {
        val (totalBalance, holdBalance) = calculateBalances(postMigrationEntries, allTransactions)
        results.add(EstablishmentBalance(prisonId, totalBalance, holdBalance))
      }
    }

    return results.toList()
  }

  /**
   * Finds the timestamp of the latest Opening Balance ('OB') or Hold Opening Balance ('OHB') transaction
   * for a given account AND prison.
   */
  private fun findLatestMigrationTimestampForPrison(accountId: Long, prisonCode: String, allTransactions: Map<Long, Transaction>): Instant? {
    val transactionIds = transactionEntryRepository.findByAccountId(accountId).map { it.transactionId }.distinct()

    return allTransactions
      .filterKeys { it in transactionIds }
      .values
      .filter { it.transactionType in migrationTypes && it.prison == prisonCode }
      .maxOfOrNull { it.date.toInstant() }
  }

  /**
   * Contains the core logic for calculating total and hold balances
   */
  private fun calculateBalances(transactionEntries: List<TransactionEntry>, allTransactions: Map<Long, Transaction>): Pair<BigDecimal, BigDecimal> {
    // Exclude OHB as it represents an opening balance for holds, not net cash movement.
    val nonHoldEntries = transactionEntries.filter { entry ->
      allTransactions[entry.transactionId]?.transactionType != "OHB"
    }

    val totalBalance = nonHoldEntries.sumOf { entry ->
      when (entry.entryType) {
        PostingType.CR -> entry.amount
        PostingType.DR -> entry.amount.negate()
      }
    }

    val holdEntries = transactionEntries.filter { entry ->
      allTransactions[entry.transactionId]?.transactionType in holdTransactionTypes
    }

    val holdBalance = holdEntries.sumOf { entry ->
      when (allTransactions[entry.transactionId]?.transactionType) {
        "HOA", "OHB" -> entry.amount
        "HOR" -> entry.amount.negate()
        else -> BigDecimal.ZERO
      }
    }

    return Pair(totalBalance, holdBalance)
  }
}
