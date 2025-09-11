package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(description = "Represents a single general ledger account and its initial balance for migration.")
data class InitialGeneralLedgerBalance(
  @field:NotNull
  @field:Schema(description = "The account code for the general ledger account.")
  val accountCode: Int,

  @field:NotNull
  @field:Schema(description = "The initial balance for the general ledger account.", example = "123.45")
  val balance: BigDecimal,
)
