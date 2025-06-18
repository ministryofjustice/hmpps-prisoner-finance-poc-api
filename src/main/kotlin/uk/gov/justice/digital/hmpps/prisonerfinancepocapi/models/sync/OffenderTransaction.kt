package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Represents a single offender transaction entry, including related general ledger entries.")
data class OffenderTransaction(
  @Schema(description = "The sequence number for this specific offender transaction entry.", example = "1", required = true)
  val entrySequence: Long,

  @Schema(description = "The internal ID of the offender.", example = "1015388", required = true)
  val offenderId: Long,

  @Schema(description = "The display ID for the offender.", example = "AA001AA", required = true)
  val offenderDisplayId: String,

  @Schema(description = "The booking ID of the offender transaction.", example = "455987", required = false, nullable = true)
  val offenderBookingId: Long?,

  @Schema(description = "The sub-account type (e.g., REG, SPND, SAV).", example = "REG", required = true)
  val subAccountType: String,

  @Schema(description = "The type of posting (DR/CR).", allowableValues = ["DR", "CR"], example = "DR", required = true)
  val postingType: String,

  @Schema(description = "The type of transaction (e.g., OT, DISCP).", example = "OT", required = true)
  val type: String,

  @Schema(description = "A description of the transaction entry.", example = "Sub-Account Transfer", required = true)
  val description: String,

  @Schema(description = "The monetary amount of the transaction entry.", example = "162.00", required = true)
  val amount: Double,

  @Schema(description = "An optional reference number for the transaction.", example = "", nullable = true)
  val reference: String?,

  @Schema(description = "A list of general ledger entries associated with this offender transaction.", required = true)
  val generalLedgerEntries: List<GeneralLedgerEntry>,
)
