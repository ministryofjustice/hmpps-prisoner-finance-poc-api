package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

@Schema(description = "A request to migrate initial general ledger balances for a single prison.")
data class InitialGeneralLedgerBalancesRequest(
  @field:Valid
  @field:NotEmpty
  @field:Schema(description = "A list of general ledger account balances to be initialized.")
  val initialBalances: List<InitialGeneralLedgerBalance>,
)
