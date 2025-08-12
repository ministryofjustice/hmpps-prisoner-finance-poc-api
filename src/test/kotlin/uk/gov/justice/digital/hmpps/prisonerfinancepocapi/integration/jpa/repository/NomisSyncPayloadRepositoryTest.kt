package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.jpa.repository

import org.assertj.core.api.Assertions
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

  @BeforeEach
  fun setup() {
    nomisSyncPayloadRepository.deleteAll()
    entityManager.flush()

    val now = LocalDateTime.now()

    payload1 = NomisSyncPayload(
      timestamp = now.minusHours(2),
      transactionId = 1001,
      requestId = UUID.fromString("a1a1a1a1-b1b1-c1c1-d1d1-e1e1e1e1e1e1"),
      caseloadId = "MDI",
      requestTypeIdentifier = requestType1,
      body = """{"transactionId":1001,"caseloadId":"MDI","offenderId":123,"eventType":"SyncOffenderTransaction"}""",
    )
    entityManager.persistAndFlush(payload1)

    payload2 = NomisSyncPayload(
      timestamp = now,
      transactionId = 1002,
      requestId = UUID.fromString("a3a3a3a3-b3b3-c3c3-d3d3-e3e3e3e3e3e3"),
      caseloadId = "LEI",
      requestTypeIdentifier = requestType1,
      body = """{"transactionId":1003,"caseloadId":"LEI","offenderId":456,"eventType":"SyncOffenderTransaction"}""",
    )
    entityManager.persistAndFlush(payload2)
  }

  @Nested
  @DisplayName("save")
  inner class Save {
    @Test
    fun `should save a NomisSyncPayload`() {
      val newPayload = NomisSyncPayload(
        timestamp = LocalDateTime.now(),
        transactionId = 1003,
        requestId = UUID.randomUUID(),
        caseloadId = "DTI",
        requestTypeIdentifier = "NewSyncType",
        body = """{"new":"data"}""",
      )

      val savedPayload = nomisSyncPayloadRepository.save(newPayload)
      entityManager.flush()
      entityManager.clear()

      Assertions.assertThat(savedPayload.id).isNotNull()
      val retrievedPayload = nomisSyncPayloadRepository.findById(savedPayload.id!!).orElse(null)
      Assertions.assertThat(retrievedPayload).isNotNull()

      Assertions.assertThat(retrievedPayload?.id).isEqualTo(savedPayload.id)
      Assertions.assertThat(retrievedPayload?.transactionId).isEqualTo(savedPayload.transactionId)
      Assertions.assertThat(retrievedPayload?.requestId).isEqualTo(savedPayload.requestId)
      Assertions.assertThat(retrievedPayload?.caseloadId).isEqualTo(savedPayload.caseloadId)
      Assertions.assertThat(retrievedPayload?.requestTypeIdentifier).isEqualTo(savedPayload.requestTypeIdentifier)
      Assertions.assertThat(retrievedPayload?.body).isEqualTo(newPayload.body)
      Assertions.assertThat(retrievedPayload?.timestamp).isCloseTo(newPayload.timestamp, Assertions.byLessThan(1, ChronoUnit.MICROS))
    }
  }

  @Nested
  @DisplayName("findByRequestTypeIdentifier")
  inner class FindByRequestTypeIdentifier {
    @Test
    fun `should find payloads by request type identifier`() {
      val found = nomisSyncPayloadRepository.findByRequestTypeIdentifier(requestType1)
      Assertions.assertThat(found).hasSize(2)
      Assertions.assertThat(found).containsExactlyInAnyOrder(payload1, payload2)
    }

    @Test
    fun `should return empty list if request type identifier not found`() {
      val found = nomisSyncPayloadRepository.findByRequestTypeIdentifier("NonExistentType")
      Assertions.assertThat(found).isEmpty()
    }
  }

  @Nested
  @DisplayName("findByRequestId")
  inner class FindByRequestId {
    @Test
    fun `should find payload by request ID`() {
      val found = nomisSyncPayloadRepository.findByRequestId(payload2.requestId!!)
      Assertions.assertThat(found).hasSize(1)
      Assertions.assertThat(found[0]).isEqualTo(payload2)
    }

    @Test
    fun `should return empty list if request ID not found`() {
      val found = nomisSyncPayloadRepository.findByRequestId(UUID.randomUUID())
      Assertions.assertThat(found).isEmpty()
    }
  }

  @Nested
  @DisplayName("findByTransactionId")
  inner class FindByTransactionId {
    @Test
    fun `should find payload by transaction ID`() {
      val found = nomisSyncPayloadRepository.findByTransactionId(payload1.transactionId!!)
      Assertions.assertThat(found).hasSize(1)
      Assertions.assertThat(found[0]).isEqualTo(payload1)
    }

    @Test
    fun `should return empty list if transaction ID not found`() {
      val found = nomisSyncPayloadRepository.findByTransactionId(9999)
      Assertions.assertThat(found).isEmpty()
    }
  }

  @Nested
  @DisplayName("findByCaseloadId")
  inner class FindByCaseloadId {
    @Test
    fun `should find payload by caseload ID`() {
      val found = nomisSyncPayloadRepository.findByCaseloadId(payload1.caseloadId!!)
      Assertions.assertThat(found).hasSize(1)
      Assertions.assertThat(found[0]).isEqualTo(payload1)
    }

    @Test
    fun `should return empty list if caseload ID not found`() {
      val found = nomisSyncPayloadRepository.findByCaseloadId("XYZ")
      Assertions.assertThat(found).isEmpty()
    }
  }

  @Nested
  @DisplayName("findAll")
  inner class FindAll {
    @Test
    fun `should retrieve all payloads`() {
      val found = nomisSyncPayloadRepository.findAll()
      Assertions.assertThat(found).hasSize(2)
      Assertions.assertThat(found).containsExactlyInAnyOrder(payload1, payload2)
    }
  }
}
