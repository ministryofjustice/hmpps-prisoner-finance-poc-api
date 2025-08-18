package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import java.time.LocalDate

@Service
class SyncService(
  private val requestCaptureService: RequestCaptureService,
) {
  fun syncGeneralLedgerTransaction(
    request: SyncGeneralLedgerTransactionRequest,
  ): SyncTransactionReceipt {
    val result = requestCaptureService.captureAndStoreRequest(request)

    val synchronizedTransactionId = result.id ?: throw IllegalStateException("Captured payload ID cannot be null.")

    return SyncTransactionReceipt(
      requestId = request.requestId,
      synchronizedTransactionId = synchronizedTransactionId,
      action = SyncTransactionReceipt.Action.CREATED,
    )
  }

  fun syncOffenderTransaction(
    request: SyncOffenderTransactionRequest,
  ): SyncTransactionReceipt {
    val result = requestCaptureService.captureAndStoreRequest(request)

    val synchronizedTransactionId = result.id ?: throw IllegalStateException("Captured payload ID cannot be null.")

    return SyncTransactionReceipt(
      requestId = request.requestId,
      synchronizedTransactionId = synchronizedTransactionId,
      action = SyncTransactionReceipt.Action.CREATED,
    )
  }

  fun getGeneralLedgerTransactionsByDate(startDate: LocalDate, endDate: LocalDate): List<SyncGeneralLedgerTransactionResponse> {
    val nomisPayloads = requestCaptureService.findGeneralLedgerTransactionsByTimestampBetween(startDate, endDate)

    val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .registerModule(KotlinModule.Builder().build())

    return nomisPayloads.map { payload ->
      val request = objectMapper.readValue<SyncGeneralLedgerTransactionRequest>(payload.body)

      SyncGeneralLedgerTransactionResponse(
        transactionId = payload.id!!,
        description = request.description,
        reference = request.reference,
        caseloadId = request.caseloadId,
        transactionType = request.transactionType,
        transactionTimestamp = request.transactionTimestamp,
        createdAt = request.createdAt,
        createdBy = request.createdBy,
        createdByDisplayName = request.createdByDisplayName,
        lastModifiedAt = request.lastModifiedAt,
        lastModifiedBy = request.lastModifiedBy,
        lastModifiedByDisplayName = request.lastModifiedByDisplayName,
        generalLedgerEntries = request.generalLedgerEntries,
      )
    }
  }

  fun getGeneralLedgerTransactionById(transactionId: Long): SyncGeneralLedgerTransactionResponse? {
    val payload = requestCaptureService.findNomisSyncPayloadById(transactionId)

    if (payload != null && payload.requestTypeIdentifier == SyncGeneralLedgerTransactionRequest::class.simpleName) {
      val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

      val request = objectMapper.readValue<SyncGeneralLedgerTransactionRequest>(payload.body)

      return SyncGeneralLedgerTransactionResponse(
        transactionId = transactionId,
        description = request.description,
        reference = request.reference,
        caseloadId = request.caseloadId,
        transactionType = request.transactionType,
        transactionTimestamp = request.transactionTimestamp,
        createdAt = request.createdAt,
        createdBy = request.createdBy,
        createdByDisplayName = request.createdByDisplayName,
        lastModifiedAt = request.lastModifiedAt,
        lastModifiedBy = request.lastModifiedBy,
        lastModifiedByDisplayName = request.lastModifiedByDisplayName,
        generalLedgerEntries = request.generalLedgerEntries,
      )
    }
    return null
  }

  fun getOffenderTransactionById(transactionId: Long): SyncOffenderTransactionResponse? {
    val payload = requestCaptureService.findNomisSyncPayloadById(transactionId)

    if (payload != null && payload.requestTypeIdentifier == SyncOffenderTransactionRequest::class.simpleName) {
      val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

      val request = objectMapper.readValue<SyncOffenderTransactionRequest>(payload.body)

      return SyncOffenderTransactionResponse(
        transactionId = request.transactionId,
        caseloadId = request.caseloadId,
        transactionTimestamp = request.transactionTimestamp,
        createdAt = request.createdAt,
        createdBy = request.createdBy,
        createdByDisplayName = request.createdByDisplayName,
        lastModifiedAt = request.lastModifiedAt,
        lastModifiedBy = request.lastModifiedBy,
        lastModifiedByDisplayName = request.lastModifiedByDisplayName,
        transactions = request.offenderTransactions,
      )
    }
    return null
  }
}
