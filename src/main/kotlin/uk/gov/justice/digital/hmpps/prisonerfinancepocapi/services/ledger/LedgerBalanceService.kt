package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionEntryRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.PrisonerEstablishmentBalance
import java.math.BigDecimal
import java.time.Instant

@Service
class LedgerBalanceService(
  private val transactionRepository: TransactionRepository,
  private val transactionEntryRepository: TransactionEntryRepository,
  private val prisonRepository: PrisonRepository,
  private val migrationFilterService: MigrationFilterService,
  private val accountRepository: AccountRepository,
) {

  private val prisonerSubAccountCodes = listOf(2101, 2102, 2103)
  private val holdCreditTransactionTypes = setOf("HOA", "WHF", "OHB")
  private val holdDebitTransactionTypes = setOf("HOR", "WFR")
  private val holdTransactionTypes = holdCreditTransactionTypes + holdDebitTransactionTypes

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
      return calculateGLBalanceFromPrisonerTransactions(account)
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
   * Calculates the balance for Prisoner General Ledger Accounts (2101, 2102, 2103) for a specific prison.
   * It starts with the GL account's determined initial balance, then aggregates all subsequent offender
   * transactions that occurred at that prison. Migration transactions are excluded from the aggregation.
   */
  private fun calculateGLBalanceFromPrisonerTransactions(account: Account): BigDecimal {
    val accountId = account.id!!
    val targetAccountCode = account.accountCode

    val prisonCode = prisonRepository.findById(account.prisonId!!).orElse(null)?.code
      ?: throw IllegalStateException("Prison not found for ID: ${account.prisonId}")

    val (glEntries, glTransactions) = retrieveAccountTransactionData(accountId)

    val glMigrationInfo = migrationFilterService.findLatestMigrationInfo(accountId, glTransactions)

    val initialBalance = determineOpeningGLBalance(account, glEntries, glTransactions, glMigrationInfo)
    val cutoffDate = glMigrationInfo?.transactionDate ?: Instant.EPOCH

    val prisonerSubAccounts = accountRepository.findByAccountCodeAndPrisonNumberIsNotNull(accountCode = targetAccountCode)

    val netTransactionsTotal = calculateNetBalanceAdjustment(
      prisonerSubAccounts = prisonerSubAccounts,
      prisonCode = prisonCode,
      cutoffDate = cutoffDate,
    )

    return initialBalance + netTransactionsTotal
  }

  /**
   * Calculates the net effect of all offender transactions occurring after the latest balance date at the specified prison,
   * excluding migration transactions.
   */
  private fun calculateNetBalanceAdjustment(
    prisonerSubAccounts: List<Account>,
    prisonCode: String,
    cutoffDate: Instant,
  ): BigDecimal = prisonerSubAccounts.sumOf { prisonerAccount ->
    val (prisonerEntries, prisonerTransactions) = retrieveAccountTransactionData(prisonerAccount.id!!)

    prisonerEntries
      .mapNotNull { entry ->
        val transaction = prisonerTransactions[entry.transactionId]
        val transactionInstant = transaction?.date?.toInstant()
        if (transaction != null && transactionInstant != null) {
          Triple(entry, transaction, transactionInstant)
        } else {
          null
        }
      }
      .filter { (_, transaction, _) -> transaction.prison == prisonCode }
      .filter { (_, transaction, _) ->
        transaction.transactionType !in migrationFilterService.migrationTypes
      }
      .filter { (_, _, transactionInstant) -> !transactionInstant.isBefore(cutoffDate) }
      .sumOf { (entry, _, _) ->
        val isNaturalPosting = (entry.entryType == prisonerAccount.postingType)
        if (isNaturalPosting) entry.amount else entry.amount.negate()
      }
  }

  /**
   * Calculates the resulting General Ledger opening balance by aggregating entries
   * belonging to the latest migration transaction.
   */
  private fun determineOpeningGLBalance(
    account: Account,
    transactionEntries: List<TransactionEntry>,
    allTransactions: Map<Long, Transaction>,
    glMigrationInfo: MigrationFilterService.LatestMigrationInfo?,
  ): BigDecimal {
    if (glMigrationInfo == null) {
      return BigDecimal.ZERO
    }

    val latestMigrationCreatedAt = glMigrationInfo.createdAt

    return transactionEntries
      .filter { entry ->
        allTransactions[entry.transactionId]?.createdAt?.equals(latestMigrationCreatedAt) ?: false
      }
      .sumOf { entry ->
        val isNaturalPosting = (entry.entryType == account.postingType)
        if (isNaturalPosting) entry.amount else entry.amount.negate()
      }
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
      allTransactions[entry.transactionId]?.prison?.let { prisonCode ->
        entry to prisonCode
      }
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
        in holdCreditTransactionTypes -> entry.amount
        in holdDebitTransactionTypes -> entry.amount.negate()
        else -> BigDecimal.ZERO
      }
    }

    return Pair(totalBalance, holdBalance)
  }
}
