package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A response containing a list of general ledger transactions.")
data class SyncGeneralLedgerTransactionListResponse(
  @field:ArraySchema(
    arraySchema = Schema(description = "The list of general ledger transactions found for the specified date range."),
    schema = Schema(implementation = SyncGeneralLedgerTransactionResponse::class),
  )
  val transactions: List<SyncGeneralLedgerTransactionResponse>,
)
