package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(description = "Represents an initial prisoner balance for migration.")
data class InitialPrisonerBalance(
  @field:NotNull
  @field:Schema(description = "The account code for the general ledger account.")
  val accountCode: Int,

  @field:NotNull
  @field:Schema(description = "The initial balance for the general ledger account.", example = "123.45")
  val balance: BigDecimal,

  @field:NotNull
  @field:Schema(description = "The initial amount on hold for the sub-account.", example = "10.00")
  val holdBalance: BigDecimal,
)
