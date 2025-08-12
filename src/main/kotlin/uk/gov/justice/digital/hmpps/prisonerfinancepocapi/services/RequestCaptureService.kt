package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@Service
class RequestCaptureService(
  private val nomisSyncPayloadRepository: NomisSyncPayloadRepository,
  private val objectMapper: ObjectMapper,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun captureAndStoreRequest(
    requestBodyObject: Any,
  ): NomisSyncPayload {
    val rawBodyJson = try {
      objectMapper.writeValueAsString(requestBodyObject)
    } catch (e: Exception) {
      log.error("Could not serialize request body to JSON for capture. Type: ${requestBodyObject::class.simpleName}", e)
      "{}"
    }

    var transactionId: Long? = null
    var requestId: UUID? = null
    var caseloadId: String? = null
    var requestTypeIdentifier: String?
    var transactionTimestamp: LocalDateTime? = null
    var offenderId: Long? = null

    when (requestBodyObject) {
      is SyncOffenderTransactionRequest -> {
        transactionId = requestBodyObject.transactionId
        requestId = requestBodyObject.requestId
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = SyncOffenderTransactionRequest::class.simpleName
        offenderId = requestBodyObject.offenderTransactions.first().offenderId
        // Convert the local transaction timestamp to UTC
        val localTransactionTimestamp = requestBodyObject.transactionTimestamp
        val sourceZone = ZoneId.of("Europe/London") // Assuming source system time zone is BST
        transactionTimestamp = ZonedDateTime.of(localTransactionTimestamp, sourceZone)
          .withZoneSameInstant(ZoneOffset.UTC)
          .toLocalDateTime()
      }
      is SyncGeneralLedgerTransactionRequest -> {
        transactionId = requestBodyObject.transactionId
        requestId = requestBodyObject.requestId
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = SyncGeneralLedgerTransactionRequest::class.simpleName
        // Convert the local transaction timestamp to UTC
        val localTransactionTimestamp = requestBodyObject.transactionTimestamp
        val sourceZone = ZoneId.of("Europe/London") // Assuming source system time zone is BST
        transactionTimestamp = ZonedDateTime.of(localTransactionTimestamp, sourceZone)
          .withZoneSameInstant(ZoneOffset.UTC)
          .toLocalDateTime()
      }
      else -> {
        requestTypeIdentifier = requestBodyObject::class.simpleName
        log.warn("Unrecognized request body type for capture: ${requestBodyObject::class.simpleName}. Storing with generic identifier.")
      }
    }

    val payload = NomisSyncPayload(
      timestamp = LocalDateTime.now(ZoneOffset.UTC),
      transactionId = transactionId,
      requestId = requestId,
      caseloadId = caseloadId,
      requestTypeIdentifier = requestTypeIdentifier,
      body = rawBodyJson,
      transactionTimestamp = transactionTimestamp,
      offenderId = offenderId,
    )
    return nomisSyncPayloadRepository.save(payload)
  }

  fun getCapturedNomisSyncPayloads(
    requestType: String? = null,
    transactionId: Long? = null,
    requestId: UUID? = null,
    caseloadId: String? = null,
  ): List<NomisSyncPayload> = when {
    requestType != null -> nomisSyncPayloadRepository.findByRequestTypeIdentifier(requestType)
    transactionId != null -> nomisSyncPayloadRepository.findByTransactionId(transactionId)
    requestId != null -> nomisSyncPayloadRepository.findByRequestId(requestId)
    caseloadId != null -> nomisSyncPayloadRepository.findByCaseloadId(caseloadId)
    else -> nomisSyncPayloadRepository.findAll()
  }

  fun findNomisSyncPayloadById(transactionId: Long): NomisSyncPayload? = nomisSyncPayloadRepository.findById(transactionId).orElse(null)

  fun findGeneralLedgerTransactionsByTimestampBetween(startDate: LocalDate, endDate: LocalDate): List<NomisSyncPayload> {
    val userZone = ZoneId.of("Europe/London")

    // Convert the start and end of the user's local day to UTC
    val startOfUserDayInUtc = ZonedDateTime.of(startDate.atStartOfDay(), userZone).withZoneSameInstant(ZoneOffset.UTC)
    val endOfUserDayInUtc = ZonedDateTime.of(endDate.plusDays(1).atStartOfDay(), userZone).withZoneSameInstant(ZoneOffset.UTC)

    return nomisSyncPayloadRepository.findAllByTransactionTimestampBetweenAndRequestTypeIdentifier(
      startOfUserDayInUtc.toLocalDateTime(),
      endOfUserDayInUtc.toLocalDateTime(),
      SyncGeneralLedgerTransactionRequest::class.simpleName!!,
    )
  }
}
