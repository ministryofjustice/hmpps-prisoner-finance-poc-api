package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceReceipt
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import java.util.UUID

@Service
class SyncService {
  fun syncGeneralLedgerTransaction(request: SyncGeneralLedgerTransactionRequest): SyncTransactionReceipt = SyncTransactionReceipt(
    transactionId = request.transactionId,
    requestId = request.requestId,
    synchronizedTransactionId = UUID.randomUUID(),
    action = SyncTransactionReceipt.Action.CREATED,
  )

  fun syncGeneralLedgerBalanceReport(request: SyncGeneralLedgerBalanceRequest): SyncGeneralLedgerBalanceReceipt = SyncGeneralLedgerBalanceReceipt(
    requestId = request.requestId,
    id = UUID.randomUUID(),
    action = SyncGeneralLedgerBalanceReceipt.Action.CREATED,
  )

  fun syncOffenderTransaction(request: SyncOffenderTransactionRequest): SyncTransactionReceipt = SyncTransactionReceipt(
    transactionId = request.transactionId,
    requestId = request.requestId,
    synchronizedTransactionId = UUID.randomUUID(),
    action = SyncTransactionReceipt.Action.CREATED,
  )
}
