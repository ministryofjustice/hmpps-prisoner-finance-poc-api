package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.AccountType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountCodeLookupRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import java.math.BigDecimal

@Service
open class AccountService(
  private val accountRepository: AccountRepository,
  private val accountCodeLookupRepository: AccountCodeLookupRepository,
) {

  fun findGeneralLedgerAccount(prisonId: Long, accountCode: Int): Account? = accountRepository.findByPrisonIdAndAccountCodeAndPrisonNumberIsNull(prisonId, accountCode)

  fun findPrisonerAccount(prisonNumber: String, accountCode: Int): Account? = accountRepository.findByPrisonNumberAndAccountCode(prisonNumber, accountCode)

  @Transactional
  open fun resolveAccount(accountCode: Int, prisonNumber: String, prisonId: Long): Account {
    val accountCodeLookup = accountCodeLookupRepository.findById(accountCode)
      .orElseThrow { IllegalArgumentException("Account code lookup not found for code: $accountCode") }

    val subAccountType = getSubAccountTypeFromCode(accountCode)

    return if (subAccountType != null) {
      accountRepository.findByPrisonNumberAndSubAccountType(prisonNumber, subAccountType)
        ?: createAccount(
          prisonId = prisonId,
          name = "$prisonNumber - $subAccountType",
          accountType = AccountType.PRISONER,
          prisonNumber = prisonNumber,
          subAccountType = subAccountType,
          accountCode = accountCode,
          postingType = accountCodeLookup.postingType,
        )
    } else {
      accountRepository.findByPrisonIdAndAccountCodeAndPrisonNumberIsNull(prisonId, accountCode)
        ?: createAccount(
          prisonId = prisonId,
          name = accountCodeLookup.name,
          accountType = AccountType.GENERAL_LEDGER,
          accountCode = accountCode,
          postingType = accountCodeLookup.postingType,
        )
    }
  }

  @Transactional
  open fun createPrisonerAccountWithBalance(
    prisonNumber: String,
    accountCode: Int,
    initialBalance: BigDecimal,
    prisonId: Long,
    initialOnHold: BigDecimal = BigDecimal.ZERO,
  ): Account {
    val subAccountType = getSubAccountTypeFromCode(accountCode)
      ?: throw IllegalArgumentException("Invalid account code for prisoner account: $accountCode")

    val accountCodeLookup = accountCodeLookupRepository.findById(accountCode)
      .orElseThrow { IllegalArgumentException("Account code lookup not found for code: $accountCode") }

    return createAccount(
      prisonId = prisonId,
      name = "$prisonNumber - $subAccountType",
      accountType = AccountType.PRISONER,
      prisonNumber = prisonNumber,
      subAccountType = subAccountType,
      accountCode = accountCode,
      initialBalance = initialBalance,
      initialOnHold = initialOnHold,
      postingType = accountCodeLookup.postingType,
    )
  }

  @Transactional
  open fun createGeneralLedgerAccountWithBalance(
    prisonId: Long,
    accountCode: Int,
    initialBalance: BigDecimal,
  ): Account = createGeneralLedgerAccount(prisonId, accountCode, initialBalance)

  @Transactional
  open fun createGeneralLedgerAccount(
    prisonId: Long?,
    accountCode: Int,
    initialBalance: BigDecimal = BigDecimal.ZERO,
  ): Account {
    val accountCodeLookup = accountCodeLookupRepository.findById(accountCode)
      .orElseThrow { IllegalArgumentException("Account code lookup not found for code: $accountCode") }

    return createAccount(
      prisonId = prisonId!!,
      name = accountCodeLookup.name,
      accountCode = accountCode,
      accountType = AccountType.GENERAL_LEDGER,
      initialBalance = initialBalance,
      postingType = accountCodeLookup.postingType,
    )
  }

  @Transactional
  open fun createAccount(
    prisonId: Long,
    name: String,
    accountType: AccountType,
    accountCode: Int,
    postingType: PostingType,
    prisonNumber: String? = null,
    subAccountType: String? = null,
    initialBalance: BigDecimal = BigDecimal.ZERO,
    initialOnHold: BigDecimal = BigDecimal.ZERO,
  ): Account {
    if (accountType == AccountType.PRISONER && prisonNumber == null) {
      throw IllegalArgumentException("Offender display ID is mandatory for PRISONER accounts.")
    }

    val initialDebits = when (postingType) {
      PostingType.DR -> if (initialBalance.signum() >= 0) initialBalance else BigDecimal.ZERO
      PostingType.CR -> if (initialBalance.signum() < 0) initialBalance.abs() else BigDecimal.ZERO
    }

    val initialCredits = when (postingType) {
      PostingType.DR -> if (initialBalance.signum() < 0) initialBalance.abs() else BigDecimal.ZERO
      PostingType.CR -> if (initialBalance.signum() >= 0) initialBalance else BigDecimal.ZERO
    }

    val account = Account(
      prisonId = prisonId,
      name = name,
      accountType = accountType,
      accountCode = accountCode,
      prisonNumber = prisonNumber,
      subAccountType = subAccountType,
      initialDebits = initialDebits,
      initialCredits = initialCredits,
      initialOnHold = initialOnHold,
      totalDebits = BigDecimal.ZERO,
      totalCredits = BigDecimal.ZERO,
      totalOnHold = BigDecimal.ZERO,
      postingType = postingType,
    )
    return accountRepository.save(account)
  }

  fun getSubAccountTypeFromCode(accountCode: Int): String? = when (accountCode) {
    2101 -> "Cash"
    2102 -> "Spends"
    2103 -> "Savings"
    else -> null
  }
}
