package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
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
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SyncQueryServiceTest {

  @Mock
  private lateinit var nomisSyncPayloadRepository: NomisSyncPayloadRepository

  @Mock
  private lateinit var objectMapper: ObjectMapper

  @Mock
  private lateinit var responseMapperService: ResponseMapperService

  @InjectMocks
  private lateinit var syncQueryService: SyncQueryService

  private lateinit var dummyPayload: NomisSyncPayload
  private lateinit var dummyGeneralLedgerRequest: SyncGeneralLedgerTransactionRequest
  private lateinit var dummyOffenderTransactionRequest: SyncOffenderTransactionRequest
  private val dummySyncId: UUID = UUID.fromString("a1a1a1a1-b1b1-c1c1-d1d1-e1e1e1e1e1e1")
  private val dummyRequestId: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef")
  private val dummyTransactionId: Long = 19228028L

  @BeforeEach
  fun setupGlobalDummies() {
    dummyPayload = NomisSyncPayload(
      id = 1L,
      timestamp = LocalDateTime.now(ZoneOffset.UTC),
      transactionId = dummyTransactionId,
      requestId = dummyRequestId,
      caseloadId = "LEI",
      requestTypeIdentifier = "dummy",
      synchronizedTransactionId = dummySyncId,
      body = "{}",
    )

    dummyGeneralLedgerRequest = SyncGeneralLedgerTransactionRequest(
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

    dummyOffenderTransactionRequest = SyncOffenderTransactionRequest(
      transactionId = 19228028,
      requestId = UUID.fromString("c3d4e5f6-a7b8-9012-3456-7890abcdef01"),
      caseloadId = "GMI",
      transactionTimestamp = LocalDateTime.now(),
      createdAt = LocalDateTime.now(),
      createdBy = "JD12345",
      createdByDisplayName = "J Doe",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
      offenderTransactions = emptyList(),
    )
  }

  @Nested
  @DisplayName("findByRequestId")
  inner class FindByRequestIdTests {

    @Test
    fun `should return payload if found by requestId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findByRequestId(any())).thenReturn(listOf(dummyPayload))

      // When
      val result = syncQueryService.findByRequestId(dummyRequestId)

      // Then
      assertThat(result).isEqualTo(dummyPayload)
    }

    @Test
    fun `should return null if not found by requestId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findByRequestId(any())).thenReturn(emptyList())

      // When
      val result = syncQueryService.findByRequestId(dummyRequestId)

      // Then
      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("findByTransactionId")
  inner class FindByTransactionIdTests {

    @Test
    fun `should return payload if found by transactionId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findByTransactionId(any())).thenReturn(listOf(dummyPayload))

      // When
      val result = syncQueryService.findByTransactionId(dummyTransactionId)

      // Then
      assertThat(result).isEqualTo(dummyPayload)
    }

    @Test
    fun `should return null if not found by transactionId`() {
      // Given
      `when`(nomisSyncPayloadRepository.findByTransactionId(any())).thenReturn(emptyList())

      // When
      val result = syncQueryService.findByTransactionId(dummyTransactionId)

      // Then
      assertThat(result).isNull()
    }
  }
}
