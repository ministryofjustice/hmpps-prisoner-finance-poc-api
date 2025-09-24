package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountCodeLookupRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.TransactionDetails

@Component
class TransactionDetailsMapper(
  private val accountRepository: AccountRepository,
  private val prisonRepository: PrisonRepository,
  private val accountCodeLookupRepository: AccountCodeLookupRepository,
) {

  fun mapToTransactionDetails(transaction: Transaction, transactionEntries: List<TransactionEntry>): TransactionDetails {
    val allAccounts = accountRepository.findAllById(transactionEntries.map { it.accountId }).associateBy { it.id }

    val postings = transactionEntries.map { entry ->
      val entryAccount = allAccounts[entry.accountId]
      val accountCodeLookup = entryAccount?.let { accountCodeLookupRepository.findById(it.accountCode).orElse(null) }

      val accountDetails = entryAccount?.let {
        TransactionDetails.TransactionAccountDetails(
          code = it.accountCode,
          name = it.name,
          transactionType = transaction.transactionType,
          transactionDescription = transaction.description,
          prison = prisonRepository.findById(it.prisonId).orElse(null)?.code,
          prisoner = it.prisonNumber,
          classification = accountCodeLookup?.classification ?: "Unknown",
          postingType = entry.entryType,
        )
      }

      TransactionDetails.TransactionPosting(
        account = accountDetails,
        address = entryAccount?.name,
        postingType = entry.entryType,
        amount = entry.amount,
      )
    }

    return TransactionDetails(
      id = transaction.uuid.toString(),
      date = transaction.date.toInstant().toString(),
      type = transaction.transactionType,
      description = transaction.description,
      postings = postings,
    )
  }
}
