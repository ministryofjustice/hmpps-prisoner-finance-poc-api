package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.jpa.repository

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.util.RepositoryTest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@RepositoryTest
class NomisSyncPayloadRepositoryTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val nomisSyncPayloadRepository: NomisSyncPayloadRepository,
) {

  private val requestType1 = "SyncOffenderTransaction"
  private lateinit var payload1: NomisSyncPayload
  private lateinit var payload2: NomisSyncPayload
  private lateinit var payload3: NomisSyncPayload
  private lateinit var payload4: NomisSyncPayload

  @BeforeEach
  fun setup() {
    nomisSyncPayloadRepository.deleteAll()
    entityManager.flush()

    val now = LocalDateTime.now()
    val synchronizedTransactionId1 = UUID.randomUUID()
    val synchronizedTransactionId2 = UUID.randomUUID()

    payload1 = NomisSyncPayload(
      timestamp = now.minusMinutes(10),
      legacyTransactionId = 1001,
      requestId = UUID.randomUUID(),
      caseloadId = "MDI",
      requestTypeIdentifier = requestType1,
      synchronizedTransactionId = synchronizedTransactionId1,
      body = """{"transactionId":1001,"caseloadId":"MDI","offenderId":123,"eventType":"SyncOffenderTransaction"}""",
      transactionTimestamp = now.minusDays(5),
    )
    entityManager.persistAndFlush(payload1)

    payload2 = NomisSyncPayload(
      timestamp = now.minusMinutes(5),
      legacyTransactionId = 1002,
      requestId = UUID.randomUUID(),
      caseloadId = "LEI",
      requestTypeIdentifier = requestType1,
      synchronizedTransactionId = synchronizedTransactionId2,
      body = """{"transactionId":1003,"caseloadId":"LEI","offenderId":456,"eventType":"SyncOffenderTransaction"}""",
      transactionTimestamp = now.minusDays(3),
    )
    entityManager.persistAndFlush(payload2)

    payload3 = NomisSyncPayload(
      timestamp = now.minusMinutes(15),
      legacyTransactionId = 1001,
      requestId = UUID.randomUUID(),
      caseloadId = "MDI",
      requestTypeIdentifier = requestType1,
      synchronizedTransactionId = synchronizedTransactionId1,
      body = """{"transactionId":1001,"caseloadId":"MDI","offenderId":123,"eventType":"SyncOffenderTransaction"}""",
      transactionTimestamp = now.minusDays(5),
    )
    entityManager.persistAndFlush(payload3)

    payload4 = NomisSyncPayload(
      timestamp = now.minusMinutes(2),
      legacyTransactionId = 1004,
      requestId = UUID.randomUUID(),
      caseloadId = "MDI",
      requestTypeIdentifier = "AnotherSyncType",
      synchronizedTransactionId = UUID.randomUUID(),
      body = """{"transactionId":1004,"caseloadId":"MDI","offenderId":789,"eventType":"AnotherSyncType"}""",
      transactionTimestamp = now.minusDays(1),
    )
    entityManager.persistAndFlush(payload4)
  }

  @Nested
  @DisplayName("save")
  inner class Save {
    @Test
    fun `should save a NomisSyncPayload`() {
      val newPayload = NomisSyncPayload(
        timestamp = LocalDateTime.now(),
        legacyTransactionId = 1003,
        requestId = UUID.randomUUID(),
        caseloadId = "DTI",
        requestTypeIdentifier = "NewSyncType",
        synchronizedTransactionId = UUID.randomUUID(),
        body = """{"new":"data"}""",
        transactionTimestamp = LocalDateTime.now(),
      )

      val savedPayload = nomisSyncPayloadRepository.save(newPayload)
      entityManager.flush()
      entityManager.clear()

      assertThat(savedPayload.id).isNotNull()
      val retrievedPayload = nomisSyncPayloadRepository.findById(savedPayload.id!!).orElse(null)
      assertThat(retrievedPayload).isNotNull()

      assertThat(retrievedPayload?.id).isEqualTo(savedPayload.id)
      assertThat(retrievedPayload?.legacyTransactionId).isEqualTo(savedPayload.legacyTransactionId)
      assertThat(retrievedPayload?.requestId).isEqualTo(savedPayload.requestId)
      assertThat(retrievedPayload?.caseloadId).isEqualTo(savedPayload.caseloadId)
      assertThat(retrievedPayload?.requestTypeIdentifier).isEqualTo(savedPayload.requestTypeIdentifier)
      assertThat(retrievedPayload?.body).isEqualTo(newPayload.body)
      assertThat(retrievedPayload?.timestamp).isCloseTo(newPayload.timestamp, Assertions.byLessThan(50, ChronoUnit.MILLIS))
    }
  }

  @Nested
  @DisplayName("findByRequestId")
  inner class FindByRequestId {
    @Test
    fun `should find payload by request ID`() {
      val found = nomisSyncPayloadRepository.findByRequestId(payload2.requestId!!)
      assertThat(found).isEqualTo(payload2)
    }

    @Test
    fun `should return null if request ID not found`() {
      val found = nomisSyncPayloadRepository.findByRequestId(UUID.randomUUID())
      assertThat(found).isNull()
    }
  }

  @Nested
  @DisplayName("findFirstByLegacyTransactionIdOrderByTimestampDesc")
  inner class FindByLegacyTransactionId {
    @Test
    fun `should find the latest payload by legacy transaction ID`() {
      val found = nomisSyncPayloadRepository.findFirstByLegacyTransactionIdOrderByTimestampDesc(payload1.legacyTransactionId!!)
      assertThat(found).isEqualTo(payload1)
    }

    @Test
    fun `should return null if legacy transaction ID not found`() {
      val found = nomisSyncPayloadRepository.findFirstByLegacyTransactionIdOrderByTimestampDesc(9999)
      assertThat(found).isNull()
    }
  }

  @Nested
  @DisplayName("findFirstBySynchronizedTransactionIdOrderByTimestampDesc")
  inner class FindFirstBySynchronizedTransactionIdOrderByTimestampDesc {
    @Test
    fun `should find the latest payload by synchronized transaction ID`() {
      val found = nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(payload1.synchronizedTransactionId!!)
      assertThat(found).isEqualTo(payload1)
    }

    @Test
    fun `should return null if synchronized transaction ID not found`() {
      val found = nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(UUID.randomUUID())
      assertThat(found).isNull()
    }
  }

  @Nested
  @DisplayName("findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier")
  inner class FindLatestByTransactionTimestampBetweenAndRequestTypeIdentifier {
    @Test
    fun `should find the latest payloads within the date range and by request type`() {
      val found = nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(
        LocalDateTime.now().minusDays(10),
        LocalDateTime.now(),
        requestType1,
      )
      assertThat(found).hasSize(2)
      assertThat(found).containsExactlyInAnyOrder(payload1, payload2)
    }

    @Test
    fun `should return empty list if no payloads within the date range`() {
      val found = nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(
        LocalDateTime.now().minusDays(20),
        LocalDateTime.now().minusDays(15),
        requestType1,
      )
      assertThat(found).isEmpty()
    }

    @Test
    fun `should return empty list if no payloads with the request type`() {
      val found = nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(
        LocalDateTime.now().minusDays(10),
        LocalDateTime.now(),
        "NonExistentType",
      )
      assertThat(found).isEmpty()
    }

    @Test
    fun `should only return the latest payload for each synchronizedTransactionId`() {
      val synchronizedTransactionId = UUID.randomUUID()
      val olderPayload = NomisSyncPayload(
        timestamp = LocalDateTime.now().minusMinutes(30),
        legacyTransactionId = 1005,
        requestId = UUID.randomUUID(),
        caseloadId = "XYZ",
        requestTypeIdentifier = requestType1,
        synchronizedTransactionId = synchronizedTransactionId,
        body = """{"transactionId":1005,"caseloadId":"XYZ","offenderId":901,"eventType":"SyncOffenderTransaction"}""",
        transactionTimestamp = LocalDateTime.now().minusDays(2),
      )
      entityManager.persistAndFlush(olderPayload)

      val newerPayload = NomisSyncPayload(
        timestamp = LocalDateTime.now().minusMinutes(10),
        legacyTransactionId = 1005,
        requestId = UUID.randomUUID(),
        caseloadId = "XYZ",
        requestTypeIdentifier = requestType1,
        synchronizedTransactionId = synchronizedTransactionId,
        body = """{"transactionId":1005,"caseloadId":"XYZ","offenderId":901,"eventType":"SyncOffenderTransaction", "updated": true}""",
        transactionTimestamp = LocalDateTime.now().minusDays(2),
      )
      entityManager.persistAndFlush(newerPayload)

      val found = nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(
        LocalDateTime.now().minusDays(10),
        LocalDateTime.now(),
        requestType1,
      )
      assertThat(found).hasSize(3)
      assertThat(found).containsExactlyInAnyOrder(payload1, payload2, newerPayload)
    }
  }

  @Nested
  @DisplayName("findAll")
  inner class FindAll {
    @Test
    fun `should retrieve all payloads`() {
      val found = nomisSyncPayloadRepository.findAll()
      assertThat(found).hasSize(4)
      assertThat(found).containsExactlyInAnyOrder(payload1, payload2, payload3, payload4)
    }
  }
}
