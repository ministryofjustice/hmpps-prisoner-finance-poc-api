package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

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
import org.mockito.kotlin.any
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SyncQueryServiceTest {

  @Mock
  private lateinit var nomisSyncPayloadRepository: NomisSyncPayloadRepository

  @Mock
  private lateinit var responseMapperService: ResponseMapperService

  @InjectMocks
  private lateinit var syncQueryService: SyncQueryService

  private lateinit var dummyPayload: NomisSyncPayload
  private val dummySyncId: UUID = UUID.fromString("a1a1a1a1-b1b1-c1c1-d1d1-e1e1e1e1e1e1")
  private val dummyRequestId: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef")
  private val dummyTransactionId: Long = 19228028L

  @BeforeEach
  fun setupGlobalDummies() {
    dummyPayload = NomisSyncPayload(
      id = 1L,
      timestamp = LocalDateTime.now(ZoneOffset.UTC),
      legacyTransactionId = dummyTransactionId,
      requestId = dummyRequestId,
      caseloadId = "LEI",
      requestTypeIdentifier = "dummy",
      synchronizedTransactionId = dummySyncId,
      body = "{}",
      transactionTimestamp = LocalDateTime.now(ZoneOffset.UTC),
    )
  }

  @Nested
  @DisplayName("findByRequestId")
  inner class FindByRequestIdTests {
    @Test
    fun `should return payload if found by requestId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findByRequestId(any())).thenReturn(dummyPayload)

      // When
      val result = syncQueryService.findByRequestId(dummyRequestId)

      // Then
      assertThat(result).isEqualTo(dummyPayload)
    }

    @Test
    fun `should return null if not found by requestId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findByRequestId(any())).thenReturn(null)

      // When
      val result = syncQueryService.findByRequestId(dummyRequestId)

      // Then
      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("findByLegacyTransactionId")
  inner class FindByLegacyTransactionIdTests {
    @Test
    fun `should return payload if found by legacyTransactionId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findFirstByLegacyTransactionIdOrderByTimestampDesc(any())).thenReturn(dummyPayload)

      // When
      val result = syncQueryService.findByLegacyTransactionId(dummyTransactionId)

      // Then
      assertThat(result).isEqualTo(dummyPayload)
    }

    @Test
    fun `should return null if not found by legacyTransactionId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findFirstByLegacyTransactionIdOrderByTimestampDesc(any())).thenReturn(null)

      // When
      val result = syncQueryService.findByLegacyTransactionId(dummyTransactionId)

      // Then
      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("findNomisSyncPayloadBySynchronizedTransactionId")
  inner class FindBySynchronizedTransactionIdTests {
    @Test
    fun `should return payload if found by synchronizedTransactionId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any())).thenReturn(dummyPayload)

      // When
      val result = syncQueryService.findNomisSyncPayloadBySynchronizedTransactionId(dummySyncId)

      // Then
      assertThat(result).isEqualTo(dummyPayload)
    }

    @Test
    fun `should return null if not found by synchronizedTransactionId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any())).thenReturn(null)

      // When
      val result = syncQueryService.findNomisSyncPayloadBySynchronizedTransactionId(dummySyncId)

      // Then
      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("getGeneralLedgerTransactionsByDate")
  inner class GeneralLedgerTransactionsTests {
    @Test
    fun `should return mapped general ledger transactions with pagination data`() {
      // Given
      val requestType = SyncGeneralLedgerTransactionRequest::class.simpleName!!
      val startDate = LocalDate.of(2023, 1, 1)
      val endDate = LocalDate.of(2023, 1, 31)
      val page = 0
      val size = 20

      val mockPayload = dummyPayload.copy(requestTypeIdentifier = requestType)
      val mockResponse = SyncGeneralLedgerTransactionResponse(
        synchronizedTransactionId = UUID.randomUUID(),
        legacyTransactionId = 1234L,
        description = "Test GL Transaction",
        reference = "REF123",
        caseloadId = "MDI",
        transactionType = "GL",
        transactionTimestamp = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        createdBy = "JD12345",
        createdByDisplayName = "J Doe",
        lastModifiedAt = null,
        lastModifiedBy = null,
        lastModifiedByDisplayName = null,
        generalLedgerEntries = emptyList(),
      )

      val mockPayloadPage: Page<NomisSyncPayload> = PageImpl(
        listOf(mockPayload),
        PageRequest.of(page, size),
        1,
      )

      `when`(nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(any(), any(), any(), any()))
        .thenReturn(mockPayloadPage)
      `when`(responseMapperService.mapToGeneralLedgerTransactionResponse(any()))
        .thenReturn(mockResponse)

      // When
      val result = syncQueryService.getGeneralLedgerTransactionsByDate(startDate, endDate, page, size)

      // Then
      assertThat(result.content).hasSize(1)
      assertThat(result.content[0]).isEqualTo(mockResponse)
      assertThat(result.totalElements).isEqualTo(1)
      assertThat(result.number).isEqualTo(page)
      assertThat(result.size).isEqualTo(size)
    }

    @Test
    fun `should return empty page if no general ledger transactions found`() {
      // Given
      val page = 0
      val size = 20
      val emptyPage: Page<NomisSyncPayload> = Page.empty()
      `when`(nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(any(), any(), any(), any()))
        .thenReturn(emptyPage)

      // When
      val result = syncQueryService.getGeneralLedgerTransactionsByDate(LocalDate.now(), LocalDate.now(), page, size)

      // Then
      assertThat(result).isEmpty()
      assertThat(result.totalElements).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("getOffenderTransactionsByDate")
  inner class OffenderTransactionsTests {
    @Test
    fun `should return mapped offender transactions with pagination data`() {
      // Given
      val requestType = SyncOffenderTransactionRequest::class.simpleName!!
      val startDate = LocalDate.of(2023, 1, 1)
      val endDate = LocalDate.of(2023, 1, 31)
      val page = 0
      val size = 20

      val mockPayload = dummyPayload.copy(requestTypeIdentifier = requestType)
      val mockResponse = SyncOffenderTransactionResponse(
        synchronizedTransactionId = UUID.randomUUID(),
        legacyTransactionId = 1234L,
        caseloadId = "MDI",
        transactionTimestamp = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        createdBy = "AB12345",
        createdByDisplayName = "A B",
        lastModifiedAt = null,
        lastModifiedBy = null,
        lastModifiedByDisplayName = null,
        transactions = emptyList(),
      )

      val mockPayloadPage: Page<NomisSyncPayload> = PageImpl(
        listOf(mockPayload),
        PageRequest.of(page, size),
        1,
      )
      `when`(nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(any(), any(), any(), any()))
        .thenReturn(mockPayloadPage)
      `when`(responseMapperService.mapToOffenderTransactionResponse(any()))
        .thenReturn(mockResponse)

      // When
      val result = syncQueryService.getOffenderTransactionsByDate(startDate, endDate, page, size)

      // Then
      assertThat(result.content).hasSize(1)
      assertThat(result.content[0]).isEqualTo(mockResponse)
      assertThat(result.totalElements).isEqualTo(1)
      assertThat(result.number).isEqualTo(page)
      assertThat(result.size).isEqualTo(size)
    }

    @Test
    fun `should return empty page if no offender transactions found`() {
      // Given
      val page = 0
      val size = 20
      val emptyPage: Page<NomisSyncPayload> = Page.empty()
      `when`(nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(any(), any(), any(), any()))
        .thenReturn(emptyPage)

      // When
      val result = syncQueryService.getOffenderTransactionsByDate(LocalDate.now(), LocalDate.now(), page, size)

      // Then
      assertThat(result).isEmpty()
      assertThat(result.totalElements).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("getGeneralLedgerTransactionById")
  inner class GetGeneralLedgerTransactionByIdTests {
    @Test
    fun `should return mapped general ledger transaction if found`() {
      // Given
      val requestType = SyncGeneralLedgerTransactionRequest::class.simpleName!!
      val mockPayload = dummyPayload.copy(requestTypeIdentifier = requestType)
      val mockResponse = SyncGeneralLedgerTransactionResponse(
        synchronizedTransactionId = UUID.randomUUID(),
        legacyTransactionId = 1234L,
        description = "Test GL Transaction",
        reference = "REF123",
        caseloadId = "MDI",
        transactionType = "GL",
        transactionTimestamp = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        createdBy = "JD12345",
        createdByDisplayName = "J Doe",
        lastModifiedAt = null,
        lastModifiedBy = null,
        lastModifiedByDisplayName = null,
        generalLedgerEntries = emptyList(),
      )

      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any()))
        .thenReturn(mockPayload)
      `when`(responseMapperService.mapToGeneralLedgerTransactionResponse(any()))
        .thenReturn(mockResponse)

      // When
      val result = syncQueryService.getGeneralLedgerTransactionById(UUID.randomUUID())

      // Then
      assertThat(result).isEqualTo(mockResponse)
    }

    @Test
    fun `should return null if payload not found`() {
      // Given
      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any()))
        .thenReturn(null)

      // When
      val result = syncQueryService.getGeneralLedgerTransactionById(UUID.randomUUID())

      // Then
      assertThat(result).isNull()
    }

    @Test
    fun `should return null if payload is not a General Ledger transaction`() {
      // Given
      val mockPayload = dummyPayload.copy(requestTypeIdentifier = SyncOffenderTransactionRequest::class.simpleName)

      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any()))
        .thenReturn(mockPayload)

      // When
      val result = syncQueryService.getGeneralLedgerTransactionById(UUID.randomUUID())

      // Then
      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("getOffenderTransactionById")
  inner class GetOffenderTransactionByIdTests {
    @Test
    fun `should return mapped offender transaction if found`() {
      // Given
      val requestType = SyncOffenderTransactionRequest::class.simpleName!!
      val mockPayload = dummyPayload.copy(requestTypeIdentifier = requestType)
      val mockResponse = SyncOffenderTransactionResponse(
        synchronizedTransactionId = UUID.randomUUID(),
        legacyTransactionId = 1234L,
        caseloadId = "MDI",
        transactionTimestamp = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        createdBy = "AB12345",
        createdByDisplayName = "A B",
        lastModifiedAt = null,
        lastModifiedBy = null,
        lastModifiedByDisplayName = null,
        transactions = emptyList(),
      )

      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any()))
        .thenReturn(mockPayload)
      `when`(responseMapperService.mapToOffenderTransactionResponse(any()))
        .thenReturn(mockResponse)

      // When
      val result = syncQueryService.getOffenderTransactionById(UUID.randomUUID())

      // Then
      assertThat(result).isEqualTo(mockResponse)
    }

    @Test
    fun `should return null if payload not found`() {
      // Given
      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any()))
        .thenReturn(null)

      // When
      val result = syncQueryService.getOffenderTransactionById(UUID.randomUUID())

      // Then
      assertThat(result).isNull()
    }

    @Test
    fun `should return null if payload is not an Offender transaction`() {
      // Given
      val mockPayload = dummyPayload.copy(requestTypeIdentifier = SyncGeneralLedgerTransactionRequest::class.simpleName)

      `when`(nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(any()))
        .thenReturn(mockPayload)

      // When
      val result = syncQueryService.getOffenderTransactionById(UUID.randomUUID())

      // Then
      assertThat(result).isNull()
    }
  }
}
