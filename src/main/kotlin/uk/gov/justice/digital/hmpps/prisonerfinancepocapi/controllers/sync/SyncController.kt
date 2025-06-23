package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.controllers.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.TAG_NOMIS_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceReceipt
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.SyncService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = TAG_NOMIS_SYNC)
@RestController
class SyncController(
  @Autowired private val syncService: SyncService,
) {

  @Operation(
    summary = "Synchronize offender transactions",
    description = """
      Transactions that have not been posted before will be created.
      Those that have already been posted and can be identified will be updated with metadata only.
      If the core details of a transaction have changed, the ledger will need to reverse the original transaction and post a new one.
    """,
  )
  @PostMapping(
    path = ["/sync/offender-transactions"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Offender transaction successfully created.",
        content = [Content(schema = Schema(implementation = SyncTransactionReceipt::class))],
      ),
      ApiResponse(
        responseCode = "200",
        description = "Offender transaction metadata successfully updated or processed with no new creations.",
        content = [Content(schema = Schema(implementation = SyncTransactionReceipt::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid input data.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized - requires a valid OAuth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal Server Error - An unexpected error occurred.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt", scopes = [ROLE_PRISONER_FINANCE_SYNC])
  @PreAuthorize("hasAnyAuthority('$ROLE_PRISONER_FINANCE_SYNC')")
  fun postOffenderTransaction(@Valid @RequestBody request: SyncOffenderTransactionRequest): ResponseEntity<SyncTransactionReceipt> {
    val receipt = syncService.syncOffenderTransaction(request)
    return when (receipt.action) {
      SyncTransactionReceipt.Action.CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(receipt)
      SyncTransactionReceipt.Action.UPDATED -> ResponseEntity.ok(receipt)
    }
  }

  @Operation(
    summary = "Report General Ledger Balances",
    description = "Receives and synchronizes a batch of general ledger account balances from the source system.",
  )
  @SecurityRequirement(name = "bearer-jwt", scopes = [ROLE_PRISONER_FINANCE_SYNC])
  @PostMapping(
    path = ["/sync/general-ledger-balances"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Balances successfully received and processed, implying new entries or a full overwrite.",
        content = [Content(schema = Schema(implementation = SyncGeneralLedgerBalanceReceipt::class))],
      ),
      ApiResponse(
        responseCode = "200",
        description = "Balances successfully updated or processed with no new creations, implying partial updates or reconciliation.",
        content = [Content(schema = Schema(implementation = SyncGeneralLedgerBalanceReceipt::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid input data.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized - requires a valid OAuth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal Server Error - An unexpected error occurred.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt", scopes = [ROLE_PRISONER_FINANCE_SYNC])
  @PreAuthorize("hasAnyAuthority('$ROLE_PRISONER_FINANCE_SYNC')")
  fun postGeneralLedgerBalanceReport(@Valid @RequestBody request: SyncGeneralLedgerBalanceRequest): ResponseEntity<SyncGeneralLedgerBalanceReceipt> {
    val receipt = syncService.syncGeneralLedgerBalanceReport(request)
    return when (receipt.action) {
      SyncGeneralLedgerBalanceReceipt.Action.CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(receipt)
      SyncGeneralLedgerBalanceReceipt.Action.UPDATED -> ResponseEntity.ok(receipt)
    }
  }

  @Operation(
    summary = "Synchronize general ledger transactions",
    description = """
      General ledger transactions that have not been posted before will be created.
      Those that have already been posted and can be identified will be updated with metadata only.
      If the core details of a transaction have changed, the ledger will need to reverse the original transaction and post a new one.
    """,
  )
  @PostMapping(
    path = ["/sync/general-ledger-transactions"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "General ledger transaction successfully posted.",
        content = [Content(schema = Schema(implementation = SyncTransactionReceipt::class))],
      ),
      ApiResponse(
        responseCode = "200",
        description = "General ledger transaction metadata successfully updated or processed with no new creations.",
        content = [Content(schema = Schema(implementation = SyncTransactionReceipt::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid input data.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized - requires a valid OAuth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal Server Error - An unexpected error occurred.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt", scopes = [ROLE_PRISONER_FINANCE_SYNC])
  @PreAuthorize("hasAnyAuthority('$ROLE_PRISONER_FINANCE_SYNC')")
  fun postGeneralLedgerTransaction(@Valid @RequestBody request: SyncGeneralLedgerTransactionRequest): ResponseEntity<SyncTransactionReceipt> {
    val receipt = syncService.syncGeneralLedgerTransaction(request)

    return when (receipt.action) {
      SyncTransactionReceipt.Action.CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(receipt)
      SyncTransactionReceipt.Action.UPDATED -> ResponseEntity.ok(receipt)
    }
  }
}
