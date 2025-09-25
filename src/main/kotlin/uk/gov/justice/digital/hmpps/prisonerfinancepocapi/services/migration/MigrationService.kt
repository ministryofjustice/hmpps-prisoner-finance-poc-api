package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.migration

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalancesRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialPrisonerBalancesRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.AccountService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.PrisonService
import java.math.BigDecimal

@Service
open class MigrationService(
  private val prisonService: PrisonService,
  private val accountService: AccountService,
  private val accountRepository: AccountRepository,
) {

  @Transactional
  open fun migrateGeneralLedgerBalances(prisonId: String, request: InitialGeneralLedgerBalancesRequest) {
    val prison = prisonService.getPrison(prisonId)
      ?: prisonService.createPrison(prisonId)

    request.initialBalances.forEach { balanceData ->

      val existingAccount = accountService.findGeneralLedgerAccount(prison.id!!, balanceData.accountCode)
      if (existingAccount == null) {
        accountService.createGeneralLedgerAccountWithBalance(
          prisonId = prison.id,
          accountCode = balanceData.accountCode,
          initialBalance = balanceData.balance,
        )
      } else {
        initialiseGeneralLedgerBalance(existingAccount, balanceData.balance)
      }
    }
  }

  @Transactional
  open fun migratePrisonerBalances(prisonNumber: String, request: InitialPrisonerBalancesRequest) {
    val prison = prisonService.getPrison(request.prisonId)
      ?: throw EntityNotFoundException("Prison with code ${request.prisonId} not found. A prisoner's accounts must be linked to an existing prison.")

    request.initialBalances.forEach { balanceData ->
      val existingPrisonerAccount = accountService.findPrisonerAccount(prisonNumber, balanceData.accountCode)
      if (existingPrisonerAccount == null) {
        accountService.createPrisonerAccountWithBalance(
          prisonNumber = prisonNumber,
          accountCode = balanceData.accountCode,
          initialBalance = balanceData.balance,
          prisonId = prison.id!!,
          initialOnHold = balanceData.holdBalance,
        )
      } else {
        initialisePrisonerAccountBalance(existingPrisonerAccount, balanceData.balance, balanceData.holdBalance)
      }
    }
  }

  private fun initialiseGeneralLedgerBalance(account: Account, newBalance: BigDecimal) {
    if (account.postingType == PostingType.DR) {
      account.initialDebits = newBalance
      account.initialCredits = BigDecimal.ZERO
    } else {
      account.initialCredits = newBalance
      account.initialDebits = BigDecimal.ZERO
    }

    account.initialOnHold = BigDecimal.ZERO
    account.totalDebits = BigDecimal.ZERO
    account.totalCredits = BigDecimal.ZERO
    account.totalOnHold = BigDecimal.ZERO

    accountRepository.save(account)
  }

  private fun initialisePrisonerAccountBalance(account: Account, newBalance: BigDecimal, newOnHoldBalance: BigDecimal) {
    if (newBalance.signum() < 0) {
      account.initialDebits = newBalance.abs()
      account.initialCredits = BigDecimal.ZERO
    } else {
      account.initialDebits = BigDecimal.ZERO
      account.initialCredits = newBalance
    }
    account.initialOnHold = newOnHoldBalance

    account.totalDebits = BigDecimal.ZERO
    account.totalCredits = BigDecimal.ZERO
    account.totalOnHold = BigDecimal.ZERO

    accountRepository.save(account)
  }
}
