package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Details for a single general ledger account balance.")
data class GeneralLedgerAccountBalance(
  @Schema(description = "A numerical code associated with the account, often representing its type or category.", example = "1101", required = true)
  val code: Int,

  @Schema(description = "The human-readable name of the account.", example = "Bank", required = true)
  val name: String,

  @Schema(
    description = "The balance of the account. Can be null if the balance is not available or not applicable for this account type.",
    example = "12345.67",
    nullable = true,
  )
  val balance: BigDecimal?,
)
