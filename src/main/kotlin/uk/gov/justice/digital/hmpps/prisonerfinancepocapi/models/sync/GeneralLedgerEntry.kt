package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Represents a general ledger entry.")
data class GeneralLedgerEntry(
  @Schema(description = "The sequence number for this specific general ledger entry.", example = "1", required = true)
  val entrySequence: Int,

  @Schema(description = "The general ledger account code.", example = "2101", required = true)
  val code: Int,

  @Schema(description = "Indicates whether the entry is a Debit (DR) or Credit (CR).", allowableValues = ["DR", "CR"], example = "DR", required = true)
  val postingType: String,

  @Schema(description = "The monetary amount of the general ledger entry.", example = "162.00", required = true)
  val amount: Double,
)
