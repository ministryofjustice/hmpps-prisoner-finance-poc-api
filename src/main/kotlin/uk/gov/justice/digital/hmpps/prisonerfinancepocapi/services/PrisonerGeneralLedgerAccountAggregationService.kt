package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events.TransactionRecordedEvent
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.AccountType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository

@Service
class PrisonerGeneralLedgerAccountAggregationService(
  private val accountRepository: AccountRepository,
) {
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun handleTransactionRecordedEvent(event: TransactionRecordedEvent) {
    event.savedEntries.forEach { entry ->
      val account = accountRepository.findById(entry.accountId)
        .orElseThrow { IllegalStateException("Account not found with ID: ${entry.accountId}") }

      if (account.accountType == AccountType.PRISONER) {
        val glAccount = accountRepository.findByPrisonIdAndAccountCodeAndPrisonNumberIsNull(
          account.prisonId,
          account.accountCode,
        ) ?: throw IllegalStateException("GL account for code ${account.accountCode} not found for prison ${account.prisonId}")

        when (entry.entryType) {
          PostingType.DR -> glAccount.totalDebits = glAccount.totalDebits.add(entry.amount)
          PostingType.CR -> glAccount.totalCredits = glAccount.totalCredits.add(entry.amount)
        }

        accountRepository.save(glAccount)
      }
    }
  }
}
