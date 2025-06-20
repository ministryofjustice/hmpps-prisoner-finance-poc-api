package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE__SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import java.time.OffsetDateTime
import java.util.UUID

class SyncGeneralLedgerTransactionTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `401 unauthorised`() {
    webTestClient
      .post()
      .uri("/sync/general-ledger-transactions")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    webTestClient
      .post()
      .uri("/sync/general-ledger-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("SOME_OTHER_ROLE")))
      .bodyValue(createSyncGeneralLedgerTransactionRequest())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `201 Created - when transaction is new`() {
    val newTransactionRequest = createSyncGeneralLedgerTransactionRequest()

    webTestClient
      .post()
      .uri("/sync/general-ledger-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE__SYNC)))
      .bodyValue(newTransactionRequest)
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.transactionId").isEqualTo(newTransactionRequest.transactionId)
      .jsonPath("$.action").isEqualTo("CREATED")
  }

  @Test
  fun `400 Bad Request - missing required requestId`() {
    val invalidMap = mapOf(
      "transactionId" to 1234,
      "caseloadId" to "GMI",
      "description" to "General Ledger Account Transfer",
      "transactionType" to "GJ",
      "transactionTimestamp" to OffsetDateTime.now(),
      "createdAt" to OffsetDateTime.now(),
      "createdBy" to "TESTUSER",
      "createdByDisplayName" to "Test User",
      "generalLedgerEntries" to emptyList<Any>(),
    )
    val invalidJson = objectMapper.writeValueAsString(invalidMap)

    webTestClient
      .post()
      .uri("/sync/general-ledger-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE__SYNC)))
      .bodyValue(invalidJson)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> { message ->
        assertThat(message).startsWith("Invalid request body:")
      }
      .jsonPath("$.developerMessage").value<String> { message ->
        assertThat(message).startsWith("JSON parse error:")
        assertThat(message).contains("requestId due to missing")
      }
  }

  @Test
  fun `400 Bad Request - createdBy too long`() {
    val longString = "A".repeat(33)
    val request = createSyncGeneralLedgerTransactionRequest().copy(createdBy = longString)

    webTestClient
      .post()
      .uri("/sync/general-ledger-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE__SYNC)))
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Validation failure")
      .jsonPath("$.developerMessage")
      .isEqualTo("Validation failed: createdBy: Created by must be supplied and be <= 32 characters")
  }

  private fun createSyncGeneralLedgerTransactionRequest(): SyncGeneralLedgerTransactionRequest = SyncGeneralLedgerTransactionRequest(
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
      GeneralLedgerEntry(entrySequence = 1L, code = 1101, postingType = "DR", amount = 50.00),
      GeneralLedgerEntry(entrySequence = 2L, code = 2503, postingType = "CR", amount = 50.00),
    ),
  )
}
