package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.domain.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.repository.NomisSyncPayloadRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
class RequestCaptureService(
  private val nomisSyncPayloadRepository: NomisSyncPayloadRepository,
  private val objectMapper: ObjectMapper,
) {

  fun captureAndStoreRequest(
    requestBodyObject: Any,
  ): NomisSyncPayload {
    val rawBodyJson = try {
      objectMapper.writeValueAsString(requestBodyObject)
    } catch (e: Exception) {
      println("ERROR: Could not serialize request body to JSON for capture: ${e.message}")
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
        requestTypeIdentifier = "SyncOffenderTransaction"
      }
      is SyncGeneralLedgerBalanceRequest -> {
        requestId = requestBodyObject.requestId
        requestTypeIdentifier = "SyncGeneralLedgerBalance"
      }
      is SyncGeneralLedgerTransactionRequest -> {
        transactionId = requestBodyObject.transactionId
        requestId = requestBodyObject.requestId
        caseloadId = requestBodyObject.caseloadId
        requestTypeIdentifier = "SyncGeneralLedgerTransaction"
      }
      else -> {
        requestTypeIdentifier = requestBodyObject::class.simpleName
        println("INFO: Unrecognized request body type for capture: ${requestBodyObject::class.simpleName}. Storing with generic identifier.")
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
