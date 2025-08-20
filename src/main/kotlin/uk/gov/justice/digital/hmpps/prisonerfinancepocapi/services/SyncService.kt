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
      val synchronizedTransactionId = existingPayloadByRequestId.synchronizedTransactionId
        ?: throw IllegalStateException("Synchronized TransactionId cannot be null on an existing payload.")
      return SyncTransactionReceipt(
        requestId = request.requestId,
        synchronizedTransactionId = synchronizedTransactionId,
        action = SyncTransactionReceipt.Action.PROCESSED,
      )
    }

    val existingPayloadByTransactionId = syncQueryService.findByTransactionId(request.transactionId)
    if (existingPayloadByTransactionId != null) {
      val synchronizedTransactionId = existingPayloadByTransactionId.synchronizedTransactionId
        ?: throw IllegalStateException("Synchronized TransactionId cannot be null on an existing payload.")

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
        return SyncTransactionReceipt(
          requestId = request.requestId,
          synchronizedTransactionId = synchronizedTransactionId,
          action = SyncTransactionReceipt.Action.PROCESSED,
        )
      } else {
        val updatedPayload = requestCaptureService.captureAndStoreRequest(request)
        val newSynchronizedTransactionId = updatedPayload.synchronizedTransactionId
          ?: throw IllegalStateException("Synchronized TransactionId cannot be null on an updated payload.")
        return SyncTransactionReceipt(
          requestId = request.requestId,
          synchronizedTransactionId = newSynchronizedTransactionId,
          action = SyncTransactionReceipt.Action.UPDATED,
        )
      }
    }

    val newPayload = requestCaptureService.captureAndStoreRequest(request)
    val synchronizedTransactionId = newPayload.synchronizedTransactionId
      ?: throw IllegalStateException("Synchronized TransactionId cannot be null.")
    return SyncTransactionReceipt(
      requestId = request.requestId,
      synchronizedTransactionId = synchronizedTransactionId,
      action = SyncTransactionReceipt.Action.CREATED,
    )
  }
}
