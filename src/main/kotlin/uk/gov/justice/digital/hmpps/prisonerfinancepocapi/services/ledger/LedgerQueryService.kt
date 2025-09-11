package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.AccountType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountCodeLookupRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionEntryRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.PrisonAccountDetails
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.PrisonerSubAccountDetails
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.TransactionDetails
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
open class LedgerQueryService(
  private val prisonRepository: PrisonRepository,
  private val accountRepository: AccountRepository,
  private val transactionRepository: TransactionRepository,
  private val transactionEntryRepository: TransactionEntryRepository,
  private val accountCodeLookupRepository: AccountCodeLookupRepository,
  private val transactionDetailsMapper: TransactionDetailsMapper,
) {

  fun getPrisonerSubAccountDetails(prisonNumber: String, accountCode: Int): PrisonerSubAccountDetails? {
    val account = findPrisonerAccount(prisonNumber, accountCode)
    return account?.let {
      PrisonerSubAccountDetails(
        code = it.accountCode,
        name = it.subAccountType ?: it.name,
        prisonNumber = prisonNumber,
        balance = calculateBalance(it),
        holdBalance = it.initialOnHold.add(it.totalOnHold),
      )
    }
  }

  fun listPrisonerSubAccountDetails(prisonNumber: String): List<PrisonerSubAccountDetails> = accountRepository.findByPrisonNumber(prisonNumber).map { account ->
    PrisonerSubAccountDetails(
      code = account.accountCode,
      name = account.subAccountType ?: account.name,
      prisonNumber = prisonNumber,
      balance = calculateBalance(account),
      holdBalance = account.initialOnHold.add(account.totalOnHold),
    )
  }

  fun getPrisonAccountDetails(prisonId: String, accountCode: Int): PrisonAccountDetails? {
    val account = findPrisonAccount(prisonId, accountCode)
    return account?.let {
      val accountCodeLookup = accountCodeLookupRepository.findById(accountCode)
        .orElseThrow { IllegalStateException("Account code lookup not found for code: $accountCode") }

      PrisonAccountDetails(
        code = it.accountCode,
        name = it.name,
        prisonId = prisonId,
        classification = accountCodeLookup.classification,
        postingType = it.postingType.name,
        balance = calculateBalance(it),
      )
    }
  }

  fun listPrisonAccountDetails(prisonId: String): List<PrisonAccountDetails> {
    val prison = prisonRepository.findByCode(prisonId) ?: return emptyList()
    val accounts = accountRepository.findByPrisonId(prison.id!!)

    return accounts.filter { it.accountType == AccountType.GENERAL_LEDGER }.map { account ->
      val accountCodeLookup = accountCodeLookupRepository.findById(account.accountCode)
        .orElseThrow { IllegalStateException("Account code lookup not found for code: ${account.accountCode}") }

      PrisonAccountDetails(
        code = account.accountCode,
        name = account.name,
        prisonId = prisonId,
        classification = accountCodeLookup.classification,
        postingType = account.postingType.name,
        balance = calculateBalance(account),
      )
    }
  }

  fun listPrisonerSubAccountTransactions(prisonNumber: String, accountCode: Int): List<TransactionDetails> {
    val account = findPrisonerAccount(prisonNumber, accountCode) ?: return emptyList()
    val transactionEntries = transactionEntryRepository.findByAccountId(account.id!!)
    if (transactionEntries.isEmpty()) {
      return emptyList()
    }
    val transactionIds = transactionEntries.map { it.transactionId }.distinct()
    val transactions = transactionRepository.findAllById(transactionIds).associateBy { it.id }
    return transactionIds.mapNotNull { transactionId ->
      val transaction = transactions[transactionId] ?: return@mapNotNull null
      val entriesForTransaction = transactionEntries.filter { it.transactionId == transactionId }
      transactionDetailsMapper.mapToTransactionDetails(transaction, entriesForTransaction)
    }
  }

  fun listPrisonAccountTransactions(prisonId: String, accountCode: Int, date: LocalDate?): List<TransactionDetails> {
    val account = findPrisonAccount(prisonId, accountCode) ?: return emptyList()
    val transactionEntries = if (date != null) {
      val dateStart = date.atStartOfDay(ZoneOffset.UTC)
      val dateEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC)
      transactionEntryRepository.findByDateBetweenAndAccountIdIn(Timestamp.from(dateStart.toInstant()), Timestamp.from(dateEnd.toInstant()), listOf(account.id!!))
    } else {
      transactionEntryRepository.findByAccountId(account.id!!)
    }
    if (transactionEntries.isEmpty()) {
      return emptyList()
    }
    val transactionIds = transactionEntries.map { it.transactionId }.distinct()
    val transactions = transactionRepository.findAllById(transactionIds).associateBy { it.id }
    return transactionIds.mapNotNull { transactionId ->
      val transaction = transactions[transactionId] ?: return@mapNotNull null
      val entriesForTransaction = transactionEntries.filter { it.transactionId == transactionId }
      transactionDetailsMapper.mapToTransactionDetails(transaction, entriesForTransaction)
    }
  }

  fun getTransaction(prisonNumber: String, accountCode: Int, transactionId: String): List<TransactionDetails> {
    val account = findPrisonerAccount(prisonNumber, accountCode) ?: return emptyList()
    val transaction = transactionRepository.findByUuid(UUID.fromString(transactionId)) ?: return emptyList()
    val transactionEntries = transactionEntryRepository.findByTransactionId(transaction.id!!)
    val isForThisAccount = transactionEntries.any { it.accountId == account.id }
    if (!isForThisAccount) {
      return emptyList()
    }
    return listOf(transactionDetailsMapper.mapToTransactionDetails(transaction, transactionEntries))
  }

  fun getPrisonAccountTransaction(prisonId: String, accountCode: Int, transactionId: String): List<TransactionDetails> {
    val account = findPrisonAccount(prisonId, accountCode) ?: return emptyList()
    val transaction = transactionRepository.findByUuid(UUID.fromString(transactionId)) ?: return emptyList()
    val transactionEntries = transactionEntryRepository.findByTransactionId(transaction.id!!)
    val isForThisAccount = transactionEntries.any { it.accountId == account.id }
    if (!isForThisAccount) {
      return emptyList()
    }
    return listOf(transactionDetailsMapper.mapToTransactionDetails(transaction, transactionEntries))
  }

  private fun calculateBalance(account: Account): BigDecimal {
    val initialNetBalance = when (account.postingType) {
      PostingType.DR -> account.initialDebits.subtract(account.initialCredits)
      PostingType.CR -> account.initialCredits.subtract(account.initialDebits)
    }

    val transactionNetBalance = when (account.postingType) {
      PostingType.DR -> account.totalDebits.subtract(account.totalCredits)
      PostingType.CR -> account.totalCredits.subtract(account.totalDebits)
    }
    return initialNetBalance.add(transactionNetBalance)
  }

  private fun findPrisonAccount(prisonId: String, accountCode: Int): Account? {
    val prison = prisonRepository.findByCode(prisonId) ?: return null
    return accountRepository.findByPrisonIdAndAccountCodeAndPrisonNumberIsNull(prison.id!!, accountCode)
  }

  private fun findPrisonerAccount(prisonNumber: String, accountCode: Int): Account? = accountRepository.findByPrisonNumberAndAccountCode(prisonNumber, accountCode)
}
