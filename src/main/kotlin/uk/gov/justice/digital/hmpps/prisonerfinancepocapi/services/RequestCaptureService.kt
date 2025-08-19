package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncRequest
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

  fun <T : SyncRequest> captureAndStoreRequest(
    requestBodyObject: T,
  ): NomisSyncPayload {
    val rawBodyJson = try {
      objectMapper.writeValueAsString(requestBodyObject)
    } catch (e: Exception) {
      log.error("Could not serialize request body to JSON for capture. Type: ${requestBodyObject::class.simpleName}", e)
      "{}"
    }

    var caseloadId: String? = null
    var requestTypeIdentifier: String?
    var transactionTimestamp: LocalDateTime? = null

    when (requestBodyObject) {
      is SyncOffenderTransactionRequest -> {
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = SyncOffenderTransactionRequest::class.simpleName
        val localTransactionTimestamp = requestBodyObject.transactionTimestamp
        val sourceZone = ZoneId.of("Europe/London")
        transactionTimestamp = ZonedDateTime.of(localTransactionTimestamp, sourceZone)
          .withZoneSameInstant(ZoneOffset.UTC)
          .toLocalDateTime()
      }
      is SyncGeneralLedgerTransactionRequest -> {
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = SyncGeneralLedgerTransactionRequest::class.simpleName
        val localTransactionTimestamp = requestBodyObject.transactionTimestamp
        val sourceZone = ZoneId.of("Europe/London")
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
      transactionId = requestBodyObject.transactionId,
      synchronizedTransactionId = UUID.randomUUID(),
      requestId = requestBodyObject.requestId,
      caseloadId = caseloadId,
      requestTypeIdentifier = requestTypeIdentifier,
      body = rawBodyJson,
      transactionTimestamp = transactionTimestamp,
    )
    return nomisSyncPayloadRepository.save(payload)
  }
}
