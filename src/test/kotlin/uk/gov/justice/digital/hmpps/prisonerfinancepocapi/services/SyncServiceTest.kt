package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncTransactionReceipt
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SyncServiceTest {

  @Mock
  private lateinit var requestCaptureService: RequestCaptureService

  @Mock
  private lateinit var syncQueryService: SyncQueryService

  @Mock
  private lateinit var objectMapper: ObjectMapper

  @InjectMocks
  private lateinit var syncService: SyncService

  private lateinit var dummyGeneralLedgerTransactionRequest: SyncGeneralLedgerTransactionRequest
  private lateinit var dummyStoredPayload: NomisSyncPayload

  @BeforeEach
  fun setupGlobalDummies() {
    dummyGeneralLedgerTransactionRequest = SyncGeneralLedgerTransactionRequest(
      transactionId = 19228029,
      requestId = UUID.fromString("c3d4e5f6-a7b8-9012-3456-7890abcdef01"),
      description = "General Ledger Account Transfer",
      reference = "REF12345",
      caseloadId = "MDI",
      transactionType = "GJ",
      transactionTimestamp = LocalDateTime.now(),
      createdAt = LocalDateTime.now(),
      createdBy = "JD12346",
      createdByDisplayName = "J. Smith",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
      generalLedgerEntries = listOf(
        GeneralLedgerEntry(entrySequence = 1, code = 1101, postingType = "DR", amount = 50.00),
        GeneralLedgerEntry(entrySequence = 2, code = 2503, postingType = "CR", amount = 50.00),
      ),
    )

    dummyStoredPayload = NomisSyncPayload(
      id = 1L,
      timestamp = LocalDateTime.now(ZoneOffset.UTC),
      transactionId = dummyGeneralLedgerTransactionRequest.transactionId,
      requestId = dummyGeneralLedgerTransactionRequest.requestId,
      caseloadId = dummyGeneralLedgerTransactionRequest.caseloadId,
      requestTypeIdentifier = SyncGeneralLedgerTransactionRequest::class.simpleName,
      synchronizedTransactionId = UUID.fromString("a1a1a1a1-b1b1-c1c1-d1d1-e1e1e1e1e1e1"),
      body = "{}",
    )
  }

  @Nested
  @DisplayName("syncTransaction")
  inner class SyncTransactionTests {

    @Test
    fun `should return PROCESSED if a request with the same requestId already exists`() {
      // Given
      `when`(syncQueryService.findByRequestId(any())).thenReturn(dummyStoredPayload)

      // When
      val result = syncService.syncTransaction(dummyGeneralLedgerTransactionRequest)

      // Then
      assertThat(result.action).isEqualTo(SyncTransactionReceipt.Action.PROCESSED)
      assertThat(result.requestId).isEqualTo(dummyGeneralLedgerTransactionRequest.requestId)
      assertThat(result.synchronizedTransactionId).isEqualTo(dummyStoredPayload.synchronizedTransactionId)
      verify(syncQueryService, times(1)).findByRequestId(any())
      verify(syncQueryService, times(0)).findByTransactionId(any())
      verify(requestCaptureService, times(0)).captureAndStoreRequest(any())
    }

    @Test
    fun `should return CREATED if neither requestId nor transactionId exists`() {
      // Given
      `when`(syncQueryService.findByRequestId(any())).thenReturn(null)
      `when`(syncQueryService.findByTransactionId(any())).thenReturn(null)
      `when`(requestCaptureService.captureAndStoreRequest(any())).thenReturn(dummyStoredPayload)

      // When
      val result = syncService.syncTransaction(dummyGeneralLedgerTransactionRequest)

      // Then
      assertThat(result.action).isEqualTo(SyncTransactionReceipt.Action.CREATED)
      assertThat(result.requestId).isEqualTo(dummyGeneralLedgerTransactionRequest.requestId)
      assertThat(result.synchronizedTransactionId).isEqualTo(dummyStoredPayload.synchronizedTransactionId)
      verify(syncQueryService, times(1)).findByRequestId(any())
      verify(syncQueryService, times(1)).findByTransactionId(any())
      verify(requestCaptureService, times(1)).captureAndStoreRequest(any())
    }

    @Nested
    @DisplayName("when a transactionId exists but requestId does not")
    inner class TransactionIdExistsTests {

      @BeforeEach
      fun setup() {
        `when`(syncQueryService.findByRequestId(any())).thenReturn(null)
        `when`(syncQueryService.findByTransactionId(any())).thenReturn(dummyStoredPayload)
      }

      @Test
      fun `should return PROCESSED if the body JSON is identical`() {
        // Given
        val newBodyJson = "{\"transactionId\":19228029,\"requestId\":\"c3d4e5f6-a7b8-9012-3456-7890abcdef01\"}"
        `when`(objectMapper.writeValueAsString(any())).thenReturn(newBodyJson)
        `when`(syncQueryService.compareJsonBodies(any(), any())).thenReturn(true)

        // When
        val result = syncService.syncTransaction(dummyGeneralLedgerTransactionRequest)

        // Then
        assertThat(result.action).isEqualTo(SyncTransactionReceipt.Action.PROCESSED)
        assertThat(result.requestId).isEqualTo(dummyGeneralLedgerTransactionRequest.requestId)
        assertThat(result.synchronizedTransactionId).isEqualTo(dummyStoredPayload.synchronizedTransactionId)
        verify(objectMapper, times(1)).writeValueAsString(any())
        verify(syncQueryService, times(1)).compareJsonBodies(any(), any())
        verify(requestCaptureService, times(0)).captureAndStoreRequest(any())
      }

      @Test
      fun `should return UPDATED if the body JSON is different`() {
        // Given
        val differentBodyJson = "{\"transactionId\":19228029,\"requestId\":\"c3d4e5f6-a7b8-9012-3456-7890abcdef01\",\"newField\":\"value\"}"
        val updatedPayload = dummyStoredPayload.copy(synchronizedTransactionId = UUID.randomUUID())
        `when`(objectMapper.writeValueAsString(any())).thenReturn(differentBodyJson)
        `when`(syncQueryService.compareJsonBodies(any(), any())).thenReturn(false)
        `when`(requestCaptureService.captureAndStoreRequest(any())).thenReturn(updatedPayload)

        // When
        val result = syncService.syncTransaction(dummyGeneralLedgerTransactionRequest)

        // Then
        assertThat(result.action).isEqualTo(SyncTransactionReceipt.Action.UPDATED)
        assertThat(result.requestId).isEqualTo(dummyGeneralLedgerTransactionRequest.requestId)
        assertThat(result.synchronizedTransactionId).isEqualTo(updatedPayload.synchronizedTransactionId)
        verify(objectMapper, times(1)).writeValueAsString(any())
        verify(syncQueryService, times(1)).compareJsonBodies(any(), any())
        verify(requestCaptureService, times(1)).captureAndStoreRequest(any())
      }
    }

    @Test
    fun `should throw IllegalStateException if synchronizedTransactionId is null on existing payload`() {
      // Given
      val payloadWithNullId = dummyStoredPayload.copy(synchronizedTransactionId = null)
      `when`(syncQueryService.findByRequestId(any())).thenReturn(payloadWithNullId)

      // When / Then
      val exception = assertThrows<IllegalStateException> {
        syncService.syncTransaction(dummyGeneralLedgerTransactionRequest)
      }
      assertThat(exception.message).isEqualTo("Synchronized TransactionId cannot be null on an existing payload.")
    }
  }
}
