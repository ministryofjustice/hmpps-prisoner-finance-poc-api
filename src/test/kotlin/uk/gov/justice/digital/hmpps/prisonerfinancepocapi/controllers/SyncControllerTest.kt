package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.controllers.sync.SyncController
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerAccountBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.OffenderTransaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceReceipt
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.SyncService
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SyncControllerTest {

  @Mock
  private lateinit var syncService: SyncService

  @InjectMocks
  private lateinit var syncController: SyncController

  private lateinit var dummyOffenderTransactionRequest: SyncOffenderTransactionRequest
  private lateinit var dummyGeneralLedgerBalanceRequest: SyncGeneralLedgerBalanceRequest
  private lateinit var dummyGeneralLedgerTransactionRequest: SyncGeneralLedgerTransactionRequest

  @BeforeEach
  fun setup() {
    dummyOffenderTransactionRequest = SyncOffenderTransactionRequest(
      transactionId = 19228028,
      requestId = UUID.randomUUID(),
      caseloadId = "GMI",
      transactionTimestamp = OffsetDateTime.now(),
      createdAt = OffsetDateTime.now(),
      createdBy = "JD12345",
      createdByDisplayName = "J Doe",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
      offenderTransactions = listOf(
        OffenderTransaction(
          entrySequence = 1,
          offenderId = 1015388L,
          offenderDisplayId = "AA001AA",
          offenderBookingId = 455987L,
          subAccountType = "REG",
          postingType = "DR",
          type = "OT",
          description = "Sub-Account Transfer",
          amount = 162.00,
          reference = null,
          generalLedgerEntries = listOf(
            GeneralLedgerEntry(entrySequence = 1, code = 2101, postingType = "DR", amount = 162.00),
            GeneralLedgerEntry(entrySequence = 2, code = 2102, postingType = "CR", amount = 162.00),
          ),
        ),
      ),
    )
    dummyGeneralLedgerBalanceRequest = SyncGeneralLedgerBalanceRequest(
      requestId = UUID.randomUUID(),
      timestamp = OffsetDateTime.now(),
      balances = listOf(
        GeneralLedgerAccountBalance(code = 1101, name = "Bank", balance = BigDecimal("12.50")),
      ),
    )
    dummyGeneralLedgerTransactionRequest = SyncGeneralLedgerTransactionRequest(
      transactionId = 19228028,
      requestId = UUID.randomUUID(),
      description = "General Ledger Account Transfer",
      reference = "REF12345",
      caseloadId = "GMI",
      transactionType = "GJ",
      transactionTimestamp = OffsetDateTime.now(),
      createdAt = OffsetDateTime.now(),
      createdBy = "JD12345",
      createdByDisplayName = "J Doe",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
      generalLedgerEntries = listOf(
        GeneralLedgerEntry(entrySequence = 1, code = 1101, postingType = "DR", amount = 50.00),
        GeneralLedgerEntry(entrySequence = 2, code = 2503, postingType = "CR", amount = 50.00),
      ),
    )
  }

  @Nested
  @DisplayName("postOffenderTransaction")
  inner class PostOffenderTransaction {
    @Test
    fun `should return CREATED status when offender transaction is new`() {
      val receipt = SyncTransactionReceipt(
        transactionId = 19228028,
        requestId = UUID.randomUUID(),
        synchronizedTransactionId = UUID.randomUUID(),
        action = SyncTransactionReceipt.Action.CREATED,
      )
      `when`(syncService.syncOffenderTransaction(dummyOffenderTransactionRequest)).thenReturn(receipt)

      val response = syncController.postOffenderTransaction(dummyOffenderTransactionRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(receipt)
    }

    @Test
    fun `should return OK status when offender transaction is updated`() {
      val receipt = SyncTransactionReceipt(
        transactionId = 19228028,
        requestId = UUID.randomUUID(),
        synchronizedTransactionId = UUID.randomUUID(),
        action = SyncTransactionReceipt.Action.UPDATED,
      )
      `when`(syncService.syncOffenderTransaction(dummyOffenderTransactionRequest)).thenReturn(receipt)

      val response = syncController.postOffenderTransaction(dummyOffenderTransactionRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(receipt)
    }
  }

  @Nested
  @DisplayName("postGeneralLedgerBalanceReport")
  inner class PostGeneralLedgerBalanceReport {
    @Test
    fun `should return CREATED status when general ledger balance report is new`() {
      val receipt = SyncGeneralLedgerBalanceReceipt(
        requestId = UUID.randomUUID(),
        id = UUID.randomUUID(),
        action = SyncGeneralLedgerBalanceReceipt.Action.CREATED,
      )
      `when`(syncService.syncGeneralLedgerBalanceReport(dummyGeneralLedgerBalanceRequest)).thenReturn(receipt)

      val response = syncController.postGeneralLedgerBalanceReport(dummyGeneralLedgerBalanceRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(receipt)
    }

    @Test
    fun `should return OK status when general ledger balance report is updated`() {
      val receipt = SyncGeneralLedgerBalanceReceipt(
        requestId = UUID.randomUUID(),
        id = UUID.randomUUID(),
        action = SyncGeneralLedgerBalanceReceipt.Action.UPDATED,
      )
      `when`(syncService.syncGeneralLedgerBalanceReport(dummyGeneralLedgerBalanceRequest)).thenReturn(receipt)

      val response = syncController.postGeneralLedgerBalanceReport(dummyGeneralLedgerBalanceRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(receipt)
    }
  }

  @Nested
  @DisplayName("postGeneralLedgerTransaction")
  inner class PostGeneralLedgerTransaction {
    @Test
    fun `should return CREATED status when general ledger transaction is new`() {
      val receipt = SyncTransactionReceipt(
        transactionId = 19228028,
        requestId = UUID.randomUUID(),
        synchronizedTransactionId = UUID.randomUUID(),
        action = SyncTransactionReceipt.Action.CREATED,
      )
      `when`(syncService.syncGeneralLedgerTransaction(dummyGeneralLedgerTransactionRequest)).thenReturn(receipt)

      val response = syncController.postGeneralLedgerTransaction(dummyGeneralLedgerTransactionRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(receipt)
    }

    @Test
    fun `should return OK status when general ledger transaction is updated`() {
      val receipt = SyncTransactionReceipt(
        transactionId = 19228028,
        requestId = UUID.randomUUID(),
        synchronizedTransactionId = UUID.randomUUID(),
        action = SyncTransactionReceipt.Action.UPDATED,
      )
      `when`(syncService.syncGeneralLedgerTransaction(dummyGeneralLedgerTransactionRequest)).thenReturn(receipt)

      val response = syncController.postGeneralLedgerTransaction(dummyGeneralLedgerTransactionRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(receipt)
    }
  }
}
