package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionEntryRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.PrisonerEstablishmentBalance
import java.math.BigDecimal

@Service
class LedgerBalanceService(
  private val transactionRepository: TransactionRepository,
  private val transactionEntryRepository: TransactionEntryRepository,
  private val prisonRepository: PrisonRepository,
  private val migrationFilterService: MigrationFilterService,
) {

  private val prisonerSubAccountCodes = listOf(2101, 2102, 2103)
  private val holdTransactionTypes = setOf("HOA", "HOR", "OHB")

  /**
   * Calculates the total and hold balances for a prisoner's account by aggregating
   * per-establishment balances.
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

  /**
   * Calculates the balance for a General Ledger account.
   * Uses aggregated data for prisoner sub-accounts, otherwise calculates from transaction entries.
   */
  fun calculateGeneralLedgerAccountBalance(account: Account): BigDecimal {
    if (account.accountCode in prisonerSubAccountCodes) {
      return calculateAggregatedPrisonerGeneralLedgerAccountBalance(account)
    }

    val (transactionEntries, allTransactions) = retrieveAccountTransactionData(account.id!!)

    val entriesSinceMigration = migrationFilterService.getPostMigrationTransactionEntries(account.id, transactionEntries, allTransactions)

    if (entriesSinceMigration.isEmpty()) {
      return BigDecimal.ZERO
    }

    return entriesSinceMigration.sumOf { entry ->
      val isNaturalPosting = (entry.entryType == account.postingType)
      if (isNaturalPosting) entry.amount else entry.amount.negate()
    }
  }

  /**
   * Calculates the total and hold balances for a prisoner's account, broken down by establishment.
   */
  fun calculatePrisonerBalancesByEstablishment(account: Account): List<PrisonerEstablishmentBalance> {
    val (transactionEntries, allTransactions) = retrieveAccountTransactionData(account.id!!)
    if (transactionEntries.isEmpty()) return emptyList()

    val entriesByPrison = groupTransactionEntriesByPrison(transactionEntries, allTransactions)
    val results = mutableListOf<PrisonerEstablishmentBalance>()

    for ((prisonCode, entries) in entriesByPrison) {
      val postMigrationEntries = migrationFilterService.getPostMigrationTransactionEntriesForPrison(
        accountId = account.id,
        prisonCode = prisonCode,
        allEntries = entries,
        allTransactions = allTransactions,
      )

      if (postMigrationEntries.isNotEmpty()) {
        val (totalBalance, holdBalance) = calculateBalances(postMigrationEntries, allTransactions)
        results.add(PrisonerEstablishmentBalance(prisonCode, totalBalance, holdBalance))
      }
    }

    return results.toList()
  }

  /**
   * Handles aggregation for prisoner general ledger accounts (2101, 2102, 2103) by delegating
   * to a repository method that sums the net balance for that prison/code combination.
   */
  private fun calculateAggregatedPrisonerGeneralLedgerAccountBalance(account: Account): BigDecimal {
    val targetAccountCode = account.accountCode
    val prisonCode = prisonRepository.findById(account.prisonId!!).orElse(null)?.code
      ?: throw IllegalStateException("Prison not found for ID: ${account.prisonId}")

    return transactionEntryRepository.calculateAggregatedBalance(
      accountCode = targetAccountCode,
      prison = prisonCode,
    ) ?: BigDecimal.ZERO
  }

  /**
   * Retrieves all transaction entries and their corresponding transactions for a given account ID.
   */
  private fun retrieveAccountTransactionData(accountId: Long): Pair<List<TransactionEntry>, Map<Long, Transaction>> {
    val transactionEntries = transactionEntryRepository.findByAccountId(accountId)
    val transactionIds = transactionEntries.map { it.transactionId }.distinct()
    val allTransactions = transactionRepository.findAllById(transactionIds).associateBy { it.id!! }
    return Pair(transactionEntries, allTransactions)
  }

  /**
   * Groups transaction entries by the prison code of their associated transaction.
   */
  private fun groupTransactionEntriesByPrison(
    transactionEntries: List<TransactionEntry>,
    allTransactions: Map<Long, Transaction>,
  ): Map<String, List<TransactionEntry>> = transactionEntries
    .mapNotNull { entry ->
      val prisonCode = allTransactions[entry.transactionId]?.prison
      if (prisonCode != null) entry to prisonCode else null
    }
    .groupBy { it.second }
    .mapValues { (_, pairs) -> pairs.map { it.first } }

  /**
   * Contains the core logic for calculating total and hold balances from a list of entries.
   */
  private fun calculateBalances(
    transactionEntries: List<TransactionEntry>,
    allTransactions: Map<Long, Transaction>,
  ): Pair<BigDecimal, BigDecimal> {
    val nonHoldEntries = transactionEntries.filter {
      allTransactions[it.transactionId]?.transactionType != "OHB"
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
