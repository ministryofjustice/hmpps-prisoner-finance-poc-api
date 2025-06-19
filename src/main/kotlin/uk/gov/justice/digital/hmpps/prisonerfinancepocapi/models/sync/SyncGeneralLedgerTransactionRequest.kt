package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Request body for synchronizing general ledger transactions. Please note these are for non-offender accounts only.")
data class SyncGeneralLedgerTransactionRequest(
  @Schema(description = "The unique identifier for the general ledger transaction.", example = "19228028", required = true)
  val transactionId: Int,

  @Schema(
    description = "A unique identifier for this synchronization request. This can be used for idempotency or tracing.",
    example = "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    required = true,
  )
  val requestId: UUID,

  @Schema(description = "A description of the general ledger transaction.", example = "General Ledger Account Transfer", required = true)
  val description: String,

  @Schema(description = "An optional reference number for the transaction.", example = "REF12345", nullable = true)
  val reference: String?,

  @Schema(description = "The ID of the caseload associated with the transaction.", example = "GMI", required = true)
  val caseloadId: String,

  @Schema(description = "The transaction type", example = "GJ", required = true)
  val transactionType: String,

  @Schema(description = "The full timestamp of the transaction (ISO 8601).", example = "2011-09-30T09:08:08.671682Z", required = true)
  val transactionTimestamp: OffsetDateTime,

  @Schema(
    description = "The date and time the transaction was created.",
    example = "2024-06-18T14:30:00.123456Z",
  )
  val createdAt: OffsetDateTime,

  @Schema(
    description = "The user ID of the person who created the transaction in the source system.",
    example = "JD12345",
  )
  @field:Size(min = 1, max = 32, message = "Created by must be supplied and be <= 32 characters")
  val createdBy: String,

  @Schema(
    description = "The display name of the person who created the transaction.",
    example = "J Doe",
  )
  @field:Size(min = 1, max = 255, message = "Created by display name must be supplied and be <= 255 characters")
  val createdByDisplayName: String,

  @Schema(
    description = "The date and time the transaction was last modified. Only provided if the transaction has been modified since creation. (ISO 8601 format with offset)",
    example = "2022-07-15T23:03:01.123456Z",
  )
  val lastModifiedAt: OffsetDateTime?,

  @Schema(
    description = "The user ID of the person who last modified the transaction. Required if lastModifiedAt has been supplied.",
    example = "AB11DZ",
  )
  @field:Size(max = 32, message = "Last modified by must be <= 32 characters")
  val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the person who last modified the transaction. Required if lastModifiedAt has been supplied.",
    example = "U Dated",
  )
  @field:Size(max = 255, message = "Last modified by display name must be <= 255 characters")
  val lastModifiedByDisplayName: String?,

  @Schema(description = "A list of general ledger entries associated with this general ledger transaction.", required = true)
  @field:Valid
  val generalLedgerEntries: List<GeneralLedgerEntry>,
)
