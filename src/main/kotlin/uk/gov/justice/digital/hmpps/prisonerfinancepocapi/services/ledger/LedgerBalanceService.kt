package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionEntryRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionRepository
import java.math.BigDecimal
import java.time.Instant

@Service
class LedgerBalanceService(
  private val transactionRepository: TransactionRepository,
  private val transactionEntryRepository: TransactionEntryRepository,
  private val prisonRepository: PrisonRepository,
) {

  private val prisonerSubAccountCodes = listOf(2101, 2102, 2103)

  /**
   * Calculates both the total balance and hold balances for a prisoner account.
   */
  fun calculatePrisonerAccountBalances(account: Account): Pair<BigDecimal, BigDecimal> {
    val transactionEntries = getPostMigrationTransactionEntries(account.id!!)

    if (transactionEntries.isEmpty()) {
      return Pair(BigDecimal.ZERO, BigDecimal.ZERO)
    }

    val transactionIds = transactionEntries.map { it.transactionId }.distinct()
    val transactions = transactionRepository.findAllById(transactionIds).associateBy { it.id }

    // Exclude OHB as it represents an opening balance for holds, not net cash movement.
    val nonHoldEntries = transactionEntries.filter { entry ->
      transactions[entry.transactionId]?.transactionType != "OHB"
    }

    val totalBalance = nonHoldEntries.sumOf { entry ->
      when (entry.entryType) {
        PostingType.CR -> entry.amount
        PostingType.DR -> entry.amount.negate()
      }
    }

    val holdTransactionTypes = listOf("HOA", "HOR", "OHB")
    val holdEntries = transactionEntries.filter { entry ->
      transactions[entry.transactionId]?.transactionType in holdTransactionTypes
    }

    val holdBalance = holdEntries.sumOf { entry ->
      when (transactions[entry.transactionId]?.transactionType) {
        "HOA", "OHB" -> entry.amount
        "HOR" -> entry.amount.negate()
        else -> BigDecimal.ZERO
      }
    }

    return Pair(totalBalance, holdBalance)
  }

  fun calculateGeneralLedgerAccountBalance(account: Account): BigDecimal {
    if (account.accountCode in prisonerSubAccountCodes) {
      return calculateAggregatedPrisonerGeneralLedgerAccountBalance(account)
    }

    val transactionEntries = getPostMigrationTransactionEntries(account.id!!)

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
   * that occurred before the latest 'OB' or 'OHB' transaction.
   */
  fun getPostMigrationTransactionEntries(accountId: Long): List<TransactionEntry> {
    val latestMigrationTimestamp = findLatestMigrationTimestamp(accountId)
    val allEntries = transactionEntryRepository.findByAccountId(accountId)

    if (latestMigrationTimestamp == null) {
      return allEntries
    }

    return allEntries.filter { entry ->
      val transaction = transactionRepository.findById(entry.transactionId).orElse(null) ?: return@filter false

      val transactionInstant = transaction.date.toInstant()

      val isMigrationTransaction = transactionInstant.equals(latestMigrationTimestamp) &&
        (transaction.transactionType == "OB" || transaction.transactionType == "OHB")

      val isPostMigration = transactionInstant.isAfter(latestMigrationTimestamp)

      isMigrationTransaction || isPostMigration
    }
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
   * Finds the timestamp of the latest Opening Balance ('OB') or Hold Opening Balance ('OHB') transaction
   * for a given account.
   */
  private fun findLatestMigrationTimestamp(accountId: Long): Instant? {
    val migrationTypes = listOf("OB", "OHB")

    val transactionIds = transactionEntryRepository.findByAccountId(accountId)
      .map { it.transactionId }
      .distinct()

    return transactionRepository.findAllById(transactionIds)
      .filter { it.transactionType in migrationTypes }
      .maxOfOrNull { it.date.toInstant() }
  }
}
