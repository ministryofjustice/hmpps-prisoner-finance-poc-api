package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.TimeConversionService
import java.math.BigDecimal
import java.util.UUID

@Service
open class LedgerSyncService(
  private val prisonService: PrisonService,
  private val accountService: AccountService,
  private val transactionService: TransactionService,
  private val timeConversionService: TimeConversionService,
) {

  @Transactional
  open fun syncOffenderTransaction(request: SyncOffenderTransactionRequest): UUID {
    if (request.offenderTransactions.isEmpty()) {
      throw IllegalArgumentException("No offender transactions provided in the request.")
    }

    val prison = prisonService.getPrison(request.caseloadId)
      ?: prisonService.createPrison(request.caseloadId)

    val transactionTimestamp = timeConversionService.toUtcInstant(request.transactionTimestamp)
    val synchronizedTransactionId = UUID.randomUUID()

    request.offenderTransactions.forEach { offenderTransaction ->
      val transactionEntries = offenderTransaction.generalLedgerEntries.map { glEntry ->
        val account = accountService.resolveAccount(
          glEntry.code,
          offenderTransaction.offenderDisplayId,
          prison.id!!,
        )
        Triple(account.id!!, BigDecimal.valueOf(glEntry.amount), PostingType.valueOf(glEntry.postingType))
      }

      transactionService.recordTransaction(
        transactionType = offenderTransaction.type,
        description = offenderTransaction.description,
        entries = transactionEntries,
        transactionTimestamp = transactionTimestamp,
        legacyTransactionId = request.transactionId,
        synchronizedTransactionId = synchronizedTransactionId,
      )
    }

    return synchronizedTransactionId
  }

  @Transactional
  open fun syncGeneralLedgerTransaction(request: SyncGeneralLedgerTransactionRequest): UUID {
    if (request.generalLedgerEntries.isEmpty()) {
      throw IllegalArgumentException("No general ledger entries provided in the request.")
    }
    val prison = prisonService.getPrison(request.caseloadId)
      ?: prisonService.createPrison(request.caseloadId)

    val transactionTimestamp = timeConversionService.toUtcInstant(request.transactionTimestamp)
    val synchronizedTransactionId = UUID.randomUUID()

    val transactionEntries = request.generalLedgerEntries.map { glEntry ->
      val account = accountService.findGeneralLedgerAccount(
        prisonId = prison.id!!,
        accountCode = glEntry.code,
      ) ?: accountService.createGeneralLedgerAccount(
        prisonId = prison.id,
        accountCode = glEntry.code,
      )
      Triple(account.id!!, BigDecimal.valueOf(glEntry.amount), PostingType.valueOf(glEntry.postingType))
    }

    transactionService.recordTransaction(
      transactionType = request.transactionType,
      description = request.description,
      entries = transactionEntries,
      transactionTimestamp = transactionTimestamp,
      legacyTransactionId = request.transactionId,
      synchronizedTransactionId = synchronizedTransactionId,
    )

    return synchronizedTransactionId
  }
}
