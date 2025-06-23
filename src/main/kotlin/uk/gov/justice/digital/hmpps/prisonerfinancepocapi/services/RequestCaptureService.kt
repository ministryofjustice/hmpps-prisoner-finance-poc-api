package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.time.LocalDateTime
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

    var transactionId: Int? = null
    var requestId: UUID? = null
    var caseloadId: String? = null
    var requestTypeIdentifier: String? = null

    when (requestBodyObject) {
      is SyncOffenderTransactionRequest -> {
        transactionId = requestBodyObject.transactionId
        requestId = requestBodyObject.requestId
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = SyncOffenderTransactionRequest::class.simpleName
      }
      is SyncGeneralLedgerBalanceRequest -> {
        requestId = requestBodyObject.requestId
        requestTypeIdentifier = SyncGeneralLedgerBalanceRequest::class.simpleName
      }
      is SyncGeneralLedgerTransactionRequest -> {
        transactionId = requestBodyObject.transactionId
        requestId = requestBodyObject.requestId
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = SyncGeneralLedgerTransactionRequest::class.simpleName
      }
      else -> {
        requestTypeIdentifier = requestBodyObject::class.simpleName
        log.warn("Unrecognized request body type for capture: ${requestBodyObject::class.simpleName}. Storing with generic identifier.")
      }
    }

    val payload = NomisSyncPayload(
      timestamp = LocalDateTime.now(),
      transactionId = transactionId,
      requestId = requestId,
      caseloadId = caseloadId,
      requestTypeIdentifier = requestTypeIdentifier,
      body = rawBodyJson,
    )
    return nomisSyncPayloadRepository.save(payload)
  }

  fun getCapturedNomisSyncPayloads(
    requestType: String? = null,
    transactionId: Int? = null,
    requestId: UUID? = null,
    caseloadId: String? = null,
  ): List<NomisSyncPayload> = when {
    requestType != null -> nomisSyncPayloadRepository.findByRequestTypeIdentifier(requestType)
    transactionId != null -> nomisSyncPayloadRepository.findByTransactionId(transactionId)
    requestId != null -> nomisSyncPayloadRepository.findByRequestId(requestId)
    caseloadId != null -> nomisSyncPayloadRepository.findByCaseloadId(caseloadId)
    else -> nomisSyncPayloadRepository.findAll()
  }
}
