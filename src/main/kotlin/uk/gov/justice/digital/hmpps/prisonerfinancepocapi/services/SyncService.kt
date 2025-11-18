package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.LedgerSyncService
import java.util.UUID

@Service
class SyncService(
  private val ledgerSyncService: LedgerSyncService,
  private val requestCaptureService: RequestCaptureService,
  private val syncQueryService: SyncQueryService,
  private val jsonComparator: JsonComparator,
  private val objectMapper: ObjectMapper,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun <T : SyncRequest> syncTransaction(
    request: T,
  ): SyncTransactionReceipt {
    val existingPayloadByRequestId = syncQueryService.findByRequestId(request.requestId)

    if (existingPayloadByRequestId != null) {
      return receipt(request, existingPayloadByRequestId.synchronizedTransactionId, action = SyncTransactionReceipt.Action.PROCESSED)
    }

    val existingPayloadByTransactionId = syncQueryService.findByLegacyTransactionId(request.transactionId)
    if (existingPayloadByTransactionId != null) {
      val newBodyJson = try {
        objectMapper.writeValueAsString(request)
      } catch (e: Exception) {
        log.error("Could not serialize new request body to JSON", e)
        "{}"
      }

      val isBodyIdentical = jsonComparator.areJsonBodiesEqual(
        storedJson = existingPayloadByTransactionId.body,
        newJson = newBodyJson,
      )

      if (isBodyIdentical) {
        return receipt(request, existingPayloadByTransactionId.synchronizedTransactionId, SyncTransactionReceipt.Action.PROCESSED)
      } else {
        val newPayload = requestCaptureService.captureAndStoreRequest(
          request,
          existingPayloadByTransactionId.synchronizedTransactionId,
        )
        return receipt(request, newPayload.synchronizedTransactionId, SyncTransactionReceipt.Action.UPDATED)
      }
    }

    var synchronizedTransactionId: UUID? = null

    try {
      when (request) {
        is SyncOffenderTransactionRequest -> {
          synchronizedTransactionId = ledgerSyncService.syncOffenderTransaction(request)
        }
        is SyncGeneralLedgerTransactionRequest -> {
          synchronizedTransactionId = ledgerSyncService.syncGeneralLedgerTransaction(request)
        }
      }
    } catch (e: Exception) {
      logRequestAsError(request, e)
      throw e
    }

    val newPayload = requestCaptureService.captureAndStoreRequest(request, synchronizedTransactionId)
    return receipt(request,newPayload.synchronizedTransactionId, SyncTransactionReceipt.Action.CREATED)
  }

  private fun receipt(
    request: SyncRequest,
    syncId: UUID,
    action: SyncTransactionReceipt.Action
  ) = SyncTransactionReceipt(
    requestId = request.requestId,
    synchronizedTransactionId = syncId,
    action = action,
  )

  private fun logRequestAsError(request: SyncRequest, exception: Exception) {
    val requestJson = try {
      objectMapper.writeValueAsString(request)
    } catch (e: Exception) {
      "Could not serialize request body to JSON for error logging: ${e.message}"
    }
    log.error("Error processing sync transaction with requestId: ${request.requestId}, transactionId: ${request.transactionId}. Request body: $requestJson", exception)
  }
}
