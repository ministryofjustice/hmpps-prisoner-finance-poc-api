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
  private val legacyTransactionFixService: LegacyTransactionFixService,
) {

  @Transactional
  open fun syncOffenderTransaction(request: SyncOffenderTransactionRequest): UUID {
    if (request.offenderTransactions.isEmpty()) {
      throw IllegalArgumentException("No offender transactions provided in the request.")
    }

    val fixedRequest = legacyTransactionFixService.fixLegacyTransactions(request)

    if (fixedRequest.offenderTransactions.isEmpty()) {
      return UUID.randomUUID()
    }

    val prison = prisonService.getPrison(fixedRequest.caseloadId)
      ?: prisonService.createPrison(fixedRequest.caseloadId)

    val transactionTimestamp = timeConversionService.toUtcInstant(fixedRequest.transactionTimestamp)
    val synchronizedTransactionId = UUID.randomUUID()

    fixedRequest.offenderTransactions.forEach { offenderTransaction ->
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
        legacyTransactionId = fixedRequest.transactionId,
        synchronizedTransactionId = synchronizedTransactionId,
        fixedRequest.caseloadId,
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
      request.caseloadId,
    )

    return synchronizedTransactionId
  }
}
