package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Request body for synchronizing general ledger balances from an external system.")
data class SyncGeneralLedgerBalanceRequest(

  @Schema(
    description = "A unique identifier for this synchronization request. This can be used for idempotency or tracing.",
    example = "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    required = true,
  )
  val requestId: UUID,

  @Schema(
    description = "The timestamp when these general ledger balances were generated or last updated in the source system (ISO 8601 format).",
    example = "2024-06-18T14:30:00.123456Z",
    required = true,
  )
  val timestamp: OffsetDateTime,

  @Schema(description = "A list of individual general ledger account balances.")
  val balances: List<GeneralLedgerAccountBalance>,
)
