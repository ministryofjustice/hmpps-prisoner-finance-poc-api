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
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
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
import java.time.OffsetDateTime
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
      transactionTimestamp = OffsetDateTime.now(),
      createdAt = OffsetDateTime.now(),
      createdBy = "JD12345",
      createdByDisplayName = "J Doe",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
      offenderTransactions = listOf(
        OffenderTransaction(
          entrySequence = 1L,
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
            GeneralLedgerEntry(entrySequence = 1L, code = 2101, postingType = "DR", amount = 162.00),
            GeneralLedgerEntry(entrySequence = 2L, code = 2102, postingType = "CR", amount = 162.00),
          ),
        ),
      ),
    )
    dummyGeneralLedgerBalanceRequest = SyncGeneralLedgerBalanceRequest(
      requestId = UUID.fromString("b2c3d4e5-f6a7-8901-2345-67890abcdef0"),
      timestamp = OffsetDateTime.now(),
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
      transactionTimestamp = OffsetDateTime.now(),
      createdAt = OffsetDateTime.now(),
      createdBy = "JD12346",
      createdByDisplayName = "J. Smith",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
      generalLedgerEntries = listOf(
        GeneralLedgerEntry(entrySequence = 1L, code = 1101, postingType = "DR", amount = 50.00),
        GeneralLedgerEntry(entrySequence = 2L, code = 2503, postingType = "CR", amount = 50.00),
      ),
    )
  }

  @Nested
  @DisplayName("captureAndStoreRequest")
  inner class CaptureAndStoreRequest {

    @BeforeEach
    fun setupSaveMock() {
      `when`(nomisSyncPayloadRepository.save(nomisSyncPayloadCaptor.capture())).thenAnswer { invocation ->
        val capturedPayload = invocation.arguments[0] as NomisSyncPayload
        capturedPayload.copy(id = 1L)
      }
    }

    @Test
    fun `should serialize and store SyncOffenderTransactionRequest with correct metadata`() {
      val expectedJson = """{"some":"json","from":"offenderTransaction"}"""
      `when`(objectMapper.writeValueAsString(dummyOffenderTransactionRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(dummyOffenderTransactionRequest)

      verify(objectMapper, times(1)).writeValueAsString(dummyOffenderTransactionRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(any(NomisSyncPayload::class.java))

      val capturedPayloadSentToRepo = nomisSyncPayloadCaptor.value
      assertThat(capturedPayloadSentToRepo.id).isNull()

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.body).isEqualTo(expectedJson)
      assertThat(result.transactionId).isEqualTo(dummyOffenderTransactionRequest.transactionId)
      assertThat(result.requestId).isEqualTo(dummyOffenderTransactionRequest.requestId)
      assertThat(result.caseloadId).isEqualTo(dummyOffenderTransactionRequest.caseloadId)
      assertThat(result.requestTypeIdentifier).isEqualTo(SyncOffenderTransactionRequest::class.simpleName)
      assertThat(result.timestamp).isCloseTo(LocalDateTime.now(), org.assertj.core.api.Assertions.within(2, ChronoUnit.SECONDS))
    }

    @Test
    fun `should serialize and store SyncGeneralLedgerBalanceRequest with correct metadata`() {
      val expectedJson = """{"some":"json","from":"generalLedgerBalance"}"""
      `when`(objectMapper.writeValueAsString(dummyGeneralLedgerBalanceRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(dummyGeneralLedgerBalanceRequest)

      verify(objectMapper, times(1)).writeValueAsString(dummyGeneralLedgerBalanceRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(any(NomisSyncPayload::class.java))

      val capturedPayloadSentToRepo = nomisSyncPayloadCaptor.value
      assertThat(capturedPayloadSentToRepo.id).isNull()

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.body).isEqualTo(expectedJson)
      assertThat(result.transactionId).isNull()
      assertThat(result.requestId).isEqualTo(dummyGeneralLedgerBalanceRequest.requestId)
      assertThat(result.caseloadId).isNull()
      assertThat(result.requestTypeIdentifier).isEqualTo(SyncGeneralLedgerBalanceRequest::class.simpleName)
      assertThat(result.timestamp).isCloseTo(LocalDateTime.now(), org.assertj.core.api.Assertions.within(2, ChronoUnit.SECONDS))
    }

    @Test
    fun `should serialize and store SyncGeneralLedgerTransactionRequest with correct metadata`() {
      val expectedJson = """{"some":"json","from":"generalLedgerTransaction"}"""
      `when`(objectMapper.writeValueAsString(dummyGeneralLedgerTransactionRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(dummyGeneralLedgerTransactionRequest)

      verify(objectMapper, times(1)).writeValueAsString(dummyGeneralLedgerTransactionRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(any(NomisSyncPayload::class.java))

      val capturedPayloadSentToRepo = nomisSyncPayloadCaptor.value
      assertThat(capturedPayloadSentToRepo.id).isNull()

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.body).isEqualTo(expectedJson)
      assertThat(result.transactionId).isEqualTo(dummyGeneralLedgerTransactionRequest.transactionId)
      assertThat(result.requestId).isEqualTo(dummyGeneralLedgerTransactionRequest.requestId)
      assertThat(result.caseloadId).isEqualTo(dummyGeneralLedgerTransactionRequest.caseloadId)
      assertThat(result.requestTypeIdentifier).isEqualTo(SyncGeneralLedgerTransactionRequest::class.simpleName)
      assertThat(result.timestamp).isCloseTo(LocalDateTime.now(), org.assertj.core.api.Assertions.within(2, ChronoUnit.SECONDS))
    }

    @Test
    fun `should handle serialization errors gracefully`() {
      `when`(objectMapper.writeValueAsString(any())).thenThrow(RuntimeException("Serialization error"))

      val result = requestCaptureService.captureAndStoreRequest("unknown_string_payload")

      verify(objectMapper, times(1)).writeValueAsString(any())
      verify(nomisSyncPayloadRepository, times(1)).save(any(NomisSyncPayload::class.java))

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.body).isEqualTo("{}")
      assertThat(result.requestTypeIdentifier).isEqualTo("String")
    }

    @Test
    fun `should store unrecognized request body type with class simpleName as identifier`() {
      class UnrecognizedRequestType
      val unrecognizedRequest = UnrecognizedRequestType()
      val expectedJson = """{}"""
      `when`(objectMapper.writeValueAsString(unrecognizedRequest)).thenReturn(expectedJson)

      val result = requestCaptureService.captureAndStoreRequest(unrecognizedRequest)

      verify(objectMapper, times(1)).writeValueAsString(unrecognizedRequest)
      verify(nomisSyncPayloadRepository, times(1)).save(any(NomisSyncPayload::class.java))

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.body).isEqualTo(expectedJson)
      assertThat(result.transactionId).isNull()
      assertThat(result.requestId).isNull()
      assertThat(result.caseloadId).isNull()
      assertThat(result.requestTypeIdentifier).isEqualTo(UnrecognizedRequestType::class.simpleName)
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
        caseloadId = "ABC",
        requestTypeIdentifier = "SyncOffenderTransaction",
        body = "{}",
      ),
      NomisSyncPayload(
        id = 2L,
        timestamp = LocalDateTime.now().minusHours(1),
        transactionId = null,
        requestId = UUID.randomUUID(),
        caseloadId = null,
        requestTypeIdentifier = "SyncGeneralLedgerBalance",
        body = "{}",
      ),
    )

    @Test
    fun `should find by requestType`() {
      `when`(nomisSyncPayloadRepository.findByRequestTypeIdentifier("SyncOffenderTransaction")).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(requestType = "SyncOffenderTransaction")

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByRequestTypeIdentifier("SyncOffenderTransaction")
      verify(nomisSyncPayloadRepository, times(0)).findByTransactionId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByRequestId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByCaseloadId(any())
      verify(nomisSyncPayloadRepository, times(0)).findAll()
    }

    @Test
    fun `should find by transactionId`() {
      `when`(nomisSyncPayloadRepository.findByTransactionId(101)).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(transactionId = 101)

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByTransactionId(101)
      verify(nomisSyncPayloadRepository, times(0)).findByRequestTypeIdentifier(any())
      verify(nomisSyncPayloadRepository, times(0)).findByRequestId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByCaseloadId(any())
      verify(nomisSyncPayloadRepository, times(0)).findAll()
    }

    @Test
    fun `should find by requestId`() {
      val targetRequestId = dummyPayloads[1].requestId!!
      `when`(nomisSyncPayloadRepository.findByRequestId(targetRequestId)).thenReturn(listOf(dummyPayloads[1]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(requestId = targetRequestId)

      assertThat(result).containsExactly(dummyPayloads[1])
      verify(nomisSyncPayloadRepository).findByRequestId(targetRequestId)
      verify(nomisSyncPayloadRepository, times(0)).findByRequestTypeIdentifier(any())
      verify(nomisSyncPayloadRepository, times(0)).findByTransactionId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByRequestId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByCaseloadId(any())
      verify(nomisSyncPayloadRepository, times(0)).findAll()
    }

    @Test
    fun `should find by caseloadId`() {
      `when`(nomisSyncPayloadRepository.findByCaseloadId("ABC")).thenReturn(listOf(dummyPayloads[0]))

      val result = requestCaptureService.getCapturedNomisSyncPayloads(caseloadId = "ABC")

      assertThat(result).containsExactly(dummyPayloads[0])
      verify(nomisSyncPayloadRepository).findByCaseloadId("ABC")
      verify(nomisSyncPayloadRepository, times(0)).findByRequestTypeIdentifier(any())
      verify(nomisSyncPayloadRepository, times(0)).findByTransactionId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByRequestId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByCaseloadId(any())
      verify(nomisSyncPayloadRepository, times(0)).findAll()
    }

    @Test
    fun `should findAll if no specific filter is provided`() {
      `when`(nomisSyncPayloadRepository.findAll()).thenReturn(dummyPayloads)

      val result = requestCaptureService.getCapturedNomisSyncPayloads()

      assertThat(result).containsExactlyInAnyOrderElementsOf(dummyPayloads)

      verify(nomisSyncPayloadRepository).findAll()
      verify(nomisSyncPayloadRepository, times(0)).findByRequestTypeIdentifier(any())
      verify(nomisSyncPayloadRepository, times(0)).findByTransactionId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByRequestId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByCaseloadId(any())
    }

    @Test
    fun `should return empty list if no results found`() {
      `when`(nomisSyncPayloadRepository.findByRequestTypeIdentifier("NonExistentType")).thenReturn(emptyList())

      val result = requestCaptureService.getCapturedNomisSyncPayloads(requestType = "NonExistentType")

      assertThat(result).isEmpty()
      verify(nomisSyncPayloadRepository).findByRequestTypeIdentifier("NonExistentType")
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
      verify(nomisSyncPayloadRepository, times(0)).findByTransactionId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByRequestId(any())
      verify(nomisSyncPayloadRepository, times(0)).findByCaseloadId(any())
      verify(nomisSyncPayloadRepository, times(0)).findAll()
    }
  }
}
