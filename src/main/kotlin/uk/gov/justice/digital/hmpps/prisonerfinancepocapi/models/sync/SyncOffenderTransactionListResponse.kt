package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A response containing a list of offender transactions.")
data class SyncOffenderTransactionListResponse(
  @field:Schema(
    description = "The list of transactions found for the specified offender and date range.",
    required = true,
  )
  val offenderTransactions: List<SyncOffenderTransactionResponse>,
)
