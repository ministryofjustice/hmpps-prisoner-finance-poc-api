package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt

@Service
class SyncService(
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
      return SyncTransactionReceipt(
        requestId = request.requestId,
        synchronizedTransactionId = existingPayloadByRequestId.synchronizedTransactionId,
        action = SyncTransactionReceipt.Action.PROCESSED,
      )
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
        // If bodies are identical, we don't need to create a new record.
        return SyncTransactionReceipt(
          requestId = request.requestId,
          synchronizedTransactionId = existingPayloadByTransactionId.synchronizedTransactionId,
          action = SyncTransactionReceipt.Action.PROCESSED,
        )
      } else {
        // If bodies are different, save the new record and use the existing synchronizedTransactionId.
        val newPayload = requestCaptureService.captureAndStoreRequest(
          request,
          existingPayloadByTransactionId.synchronizedTransactionId,
        )
        return SyncTransactionReceipt(
          requestId = request.requestId,
          synchronizedTransactionId = newPayload.synchronizedTransactionId,
          action = SyncTransactionReceipt.Action.UPDATED,
        )
      }
    }

    val newPayload = requestCaptureService.captureAndStoreRequest(request)
    return SyncTransactionReceipt(
      requestId = request.requestId,
      synchronizedTransactionId = newPayload.synchronizedTransactionId,
      action = SyncTransactionReceipt.Action.CREATED,
    )
  }
}
