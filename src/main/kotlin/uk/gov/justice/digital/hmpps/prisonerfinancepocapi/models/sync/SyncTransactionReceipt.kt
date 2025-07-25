package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Receipt details for a synchronized transaction, indicating the outcome of the operation.")
data class SyncTransactionReceipt(
  @Schema(
    description = "The unique ID of the transaction in NOMIS. This is reflected back for mapping.",
    example = "19228028",
    required = true,
  )
  var transactionId: Long,

  @Schema(
    description = "The unique identifier of the **current synchronization request**. This Id was provided by the client as the idempotency key.",
    example = "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    required = true,
  )
  var requestId: UUID,

  @Schema(
    description = "The unique UUID generated by the **finance system** for the synchronized transaction. This is the internal identifier for the created or updated record.",
    example = "f1e2d3c4-b5a6-9876-5432-10fedcba9876",
    required = true,
  )
  var synchronizedTransactionId: UUID,

  @Schema(
    description = "The action taken on the transaction by the finance system during synchronization.",
    example = "CREATED",
    required = true,
  )
  var action: Action,
) {
  @Schema(description = "The outcome of processing the transaction.")
  enum class Action {
    @Schema(description = "A new transaction was successfully created in the finance system.")
    CREATED,

    @Schema(description = "An existing transaction's metadata was successfully updated in the finance system.")
    UPDATED,
  }
}
