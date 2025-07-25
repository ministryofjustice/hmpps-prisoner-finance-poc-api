package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerAccountBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.OffenderTransaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RequestCaptureServiceTest {

  @Mock
  private lateinit var nomisSyncPayloadRepository: NomisSyncPayloadRepository

  @Mock
  private lateinit var objectMapper: ObjectMapper

  @InjectMocks
  private lateinit var requestCaptureService: RequestCaptureService

  @Captor
  private lateinit var nomisSyncPayloadCaptor: ArgumentCaptor<NomisSyncPayload>

  private lateinit var dummyOffenderTransactionRequest: SyncOffenderTransactionRequest
  private lateinit var dummyGeneralLedgerBalanceRequest: SyncGeneralLedgerBalanceRequest
  private lateinit var dummyGeneralLedgerTransactionRequest: SyncGeneralLedgerTransactionRequest

  @BeforeEach
  fun setupGlobalDummies() {
    dummyOffenderTransactionRequest = SyncOffenderTransactionRequest(
      transactionId = 19228028,
      requestId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
      caseloadId = "GMI",
      transactionTimestamp = LocalDateTime.now(),
      createdAt = LocalDateTime.now(),
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
      requestId = UUID.fromString("b2c3d4e5-f6a7-8901-2345-67890abcdef0"),
      timestamp = LocalDateTime.now(),
      balances = listOf(
        GeneralLedgerAccountBalance(code = 1101, name = "Bank", balance = BigDecimal("12.50")),
      ),
    )
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
  }

  @Nested
  @DisplayName("captureAndStoreRequest")
  inner class CaptureAndStoreRequest {

    @BeforeEach
    fun setupSaveMock() {
      `when`(nomisSyncPayloadRepository.save(any())).thenReturn(
        NomisSyncPayload(
          id = 1L,
          timestamp = LocalDateTime.now(),
          transactionId = null,
          requestId = null,
          caseloadId = null,
          requestTypeIdentifier = "Dummy",
          body = "{}",
        ),
      )
    }

    @Test
    fun `should serialize and store SyncOffenderTransactionRequest with correct metadata`() {
      val expectedJson = """{"some":"json","from":"offenderTransaction"}"""
      `when`(objectMapper.writeValueAsString(dummyOffenderTransactionRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(dummyOffenderTransactionRequest)

      verify(objectMapper, times(1)).writeValueAsString(dummyOffenderTransactionRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(nomisSyncPayloadCaptor.capture())

      val capturedPayloadSentToRepo = nomisSyncPayloadCaptor.value
      assertThat(capturedPayloadSentToRepo.id).isNull()

      assertThat(result.id).isEqualTo(1L)
      assertThat(capturedPayloadSentToRepo.body).isEqualTo(expectedJson)
      assertThat(capturedPayloadSentToRepo.transactionId).isEqualTo(dummyOffenderTransactionRequest.transactionId)
      assertThat(capturedPayloadSentToRepo.requestId).isEqualTo(dummyOffenderTransactionRequest.requestId)
      assertThat(capturedPayloadSentToRepo.caseloadId).isEqualTo(dummyOffenderTransactionRequest.caseloadId)
      assertThat(capturedPayloadSentToRepo.requestTypeIdentifier).isEqualTo(SyncOffenderTransactionRequest::class.simpleName)
      assertThat(capturedPayloadSentToRepo.timestamp).isCloseTo(LocalDateTime.now(), org.assertj.core.api.Assertions.within(2, ChronoUnit.SECONDS))
    }

    @Test
    fun `should serialize and store SyncGeneralLedgerBalanceRequest with correct metadata`() {
      val expectedJson = """{"some":"json","from":"generalLedgerBalance"}"""
      `when`(objectMapper.writeValueAsString(dummyGeneralLedgerBalanceRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(dummyGeneralLedgerBalanceRequest)

      verify(objectMapper, times(1)).writeValueAsString(dummyGeneralLedgerBalanceRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(nomisSyncPayloadCaptor.capture())

      val capturedPayloadSentToRepo = nomisSyncPayloadCaptor.value
      assertThat(capturedPayloadSentToRepo.id).isNull()

      assertThat(result.id).isEqualTo(1L)
      assertThat(capturedPayloadSentToRepo.body).isEqualTo(expectedJson)
      assertThat(capturedPayloadSentToRepo.transactionId).isNull()
      assertThat(capturedPayloadSentToRepo.requestId).isEqualTo(dummyGeneralLedgerBalanceRequest.requestId)
      assertThat(capturedPayloadSentToRepo.caseloadId).isNull()
      assertThat(capturedPayloadSentToRepo.requestTypeIdentifier).isEqualTo(SyncGeneralLedgerBalanceRequest::class.simpleName)
      assertThat(capturedPayloadSentToRepo.timestamp).isCloseTo(LocalDateTime.now(), org.assertj.core.api.Assertions.within(2, ChronoUnit.SECONDS))
    }

    @Test
    fun `should serialize and store SyncGeneralLedgerTransactionRequest with correct metadata`() {
      val expectedJson = """{"some":"json","from":"generalLedgerTransaction"}"""
      `when`(objectMapper.writeValueAsString(dummyGeneralLedgerTransactionRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(dummyGeneralLedgerTransactionRequest)

      verify(objectMapper, times(1)).writeValueAsString(dummyGeneralLedgerTransactionRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(nomisSyncPayloadCaptor.capture())

      val capturedPayloadSentToRepo = nomisSyncPayloadCaptor.value
      assertThat(capturedPayloadSentToRepo.id).isNull()

      assertThat(result.id).isEqualTo(1L)
      assertThat(capturedPayloadSentToRepo.body).isEqualTo(expectedJson)
      assertThat(capturedPayloadSentToRepo.transactionId).isEqualTo(dummyGeneralLedgerTransactionRequest.transactionId)
      assertThat(capturedPayloadSentToRepo.requestId).isEqualTo(dummyGeneralLedgerTransactionRequest.requestId)
      assertThat(capturedPayloadSentToRepo.caseloadId).isEqualTo(dummyGeneralLedgerTransactionRequest.caseloadId)
      assertThat(capturedPayloadSentToRepo.requestTypeIdentifier).isEqualTo(SyncGeneralLedgerTransactionRequest::class.simpleName)
      assertThat(capturedPayloadSentToRepo.timestamp).isCloseTo(LocalDateTime.now(), org.assertj.core.api.Assertions.within(2, ChronoUnit.SECONDS))
    }
  }

  @Nested
  @DisplayName("getCapturedNomisSyncPayloads")
  inner class GetCapturedNomisSyncPayloads {

    private val dummyPayloads = listOf(
      NomisSyncPayload(
        id = 1L,
        timestamp = LocalDateTime.now().minusDays(1),
        transactionId = 101,
        requestId = UUID.randomUUID(),
        caseloadId = "LEI",
        requestTypeIdentifier = "SyncOffenderTransaction",
        body = "{}",
      ),
      NomisSyncPayload(
        id = 2L,
        timestamp = LocalDateTime.now().minusHours(1),
        transactionId = 102,
        requestId = UUID.randomUUID(),
        caseloadId = "BRI",
        requestTypeIdentifier = "SyncGeneralLedgerBalance",
        body = "{}",
      ),
    )

    @Test
    fun `should find by requestType`() {
      `when`(nomisSyncPayloadRepository.findByRequestTypeIdentifier(any<String>())).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(requestType = "SyncOffenderTransaction")

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByRequestTypeIdentifier("SyncOffenderTransaction")
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }

    @Test
    fun `should find by transactionId`() {
      `when`(nomisSyncPayloadRepository.findByTransactionId(any<Long>())).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(transactionId = 101)

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByTransactionId(101)
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }

    @Test
    fun `should find by requestId`() {
      val targetRequestId = dummyPayloads[1].requestId!!
      `when`(nomisSyncPayloadRepository.findByRequestId(any<UUID>())).thenReturn(listOf(dummyPayloads[1]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(requestId = targetRequestId)

      assertThat(result).containsExactly(dummyPayloads[1])
      verify(nomisSyncPayloadRepository).findByRequestId(targetRequestId)
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }

    @Test
    fun `should find by caseloadId`() {
      `when`(nomisSyncPayloadRepository.findByCaseloadId(any<String>())).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(caseloadId = "ABC")

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByCaseloadId("ABC")
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }

    @Test
    fun `should findAll if no specific filter is provided`() {
      `when`(nomisSyncPayloadRepository.findAll()).thenReturn(dummyPayloads)

      val result = requestCaptureService.getCapturedNomisSyncPayloads()

      assertThat(result).containsExactlyInAnyOrderElementsOf(dummyPayloads)
      verify(nomisSyncPayloadRepository).findAll()
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }

    @Test
    fun `should return empty list if no results found`() {
      `when`(nomisSyncPayloadRepository.findByRequestTypeIdentifier("NonExistentType")).thenReturn(emptyList())

      val result = requestCaptureService.getCapturedNomisSyncPayloads(requestType = "NonExistentType")

      assertThat(result).isEmpty()
      verify(nomisSyncPayloadRepository).findByRequestTypeIdentifier("NonExistentType")
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }

    @Test
    fun `should use correct filtering precedence when multiple non-null filters are provided`() {
      `when`(nomisSyncPayloadRepository.findByRequestTypeIdentifier("SyncOffenderTransaction")).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(
        requestType = "SyncOffenderTransaction",
        transactionId = 999,
        requestId = UUID.randomUUID(),
        caseloadId = "XYZ",
      )

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByRequestTypeIdentifier("SyncOffenderTransaction")
      verifyNoMoreInteractions(nomisSyncPayloadRepository)
    }
  }
}
