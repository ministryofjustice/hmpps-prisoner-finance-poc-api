package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Request body for synchronizing an offender financial transaction.")
data class SyncOffenderTransactionRequest(
  @Schema(
    description = "The unique ID of the transaction in NOMIS.",
    example = "19228028",
    required = true,
  )
  val transactionId: Long,

  @Schema(
    description = "A unique identifier for this synchronization request. This can be used for idempotency or tracing.",
    example = "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    required = true,
  )
  val requestId: UUID,

  @Schema(
    description = "The ID of the caseload associated with this transaction.",
    example = "GMI",
    required = true,
  )
  val caseloadId: String,

  @Schema(
    description = "The timestamp when this transaction occurred (ISO 8601 format).",
    example = "2024-06-18T14:30:00.123456",
    required = true,
  )
  val transactionTimestamp: LocalDateTime,

  @Schema(
    description = "The date and time the transaction was created.",
    example = "2024-06-18T14:30:00.123456",
  )
  val createdAt: LocalDateTime,

  @Schema(
    description = "The user id of the person who created the transaction.",
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
    description = "The date and time the transaction was last modified. Only provided if the transaction has been modified since creation.",
    example = "2022-07-15T23:03:01.123456",
  )
  val lastModifiedAt: LocalDateTime?,

  @Schema(
    description = "The user id of the person who last modified the transaction. Required if lastModifiedAt has been supplied.",
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

  @Schema(
    description = "A list of individual entries that comprise this offender transaction.",
    required = true,
  )
  @field:Valid
  val offenderTransactions: List<OffenderTransaction>,
)
