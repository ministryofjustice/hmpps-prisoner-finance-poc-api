package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Service
class SyncService(
  private val requestCaptureService: RequestCaptureService,
  private val nomisSyncPayloadRepository: NomisSyncPayloadRepository,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun syncGeneralLedgerTransaction(
    request: SyncGeneralLedgerTransactionRequest,
  ): SyncTransactionReceipt {
    val result = requestCaptureService.captureAndStoreRequest(request)

    val synchronizedTransactionId = result.id ?: throw IllegalStateException("Captured payload ID cannot be null.")

    return SyncTransactionReceipt(
      transactionId = request.transactionId,
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
      transactionId = request.transactionId,
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

  fun getOffenderTransactionsByOffenderId(offenderId: Long, startDate: LocalDate?, endDate: LocalDate?): List<SyncOffenderTransactionResponse> {
    val payloads = if (startDate != null && endDate != null) {
      // Scenario 1: Date range is provided
      val queryZone = ZoneId.of("Europe/London")
      val startDateTime = startDate.atStartOfDay(queryZone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
      val endDateTime = endDate.plusDays(1).atStartOfDay(queryZone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

      nomisSyncPayloadRepository.findAllByOffenderIdAndTransactionTimestampBetweenAndRequestTypeIdentifier(
        offenderId,
        startDateTime,
        endDateTime,
        SyncOffenderTransactionRequest::class.simpleName!!,
      )
    } else {
      // Scenario 2: No date range, get all transactions for the offender
      nomisSyncPayloadRepository.findAllByOffenderIdAndRequestTypeIdentifier(
        offenderId,
        SyncOffenderTransactionRequest::class.simpleName!!,
      )
    }

    return payloads.map { payload ->

      val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

      val request = objectMapper.readValue<SyncOffenderTransactionRequest>(payload.body)

      SyncOffenderTransactionResponse(
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
  }
}
