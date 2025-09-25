package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "A request to migrate initial balances for a single prisoner across their sub-accounts.")
data class InitialPrisonerBalancesRequest(
  @field:NotBlank
  @field:Schema(description = "The prison code (e.g., 'MDI') where the prisoner is located.")
  val prisonId: String,

  @field:Valid
  @field:NotEmpty
  @field:Schema(description = "A list of sub-account balances to be initialized for the prisoner.")
  val initialBalances: List<InitialPrisonerBalance>,
)
