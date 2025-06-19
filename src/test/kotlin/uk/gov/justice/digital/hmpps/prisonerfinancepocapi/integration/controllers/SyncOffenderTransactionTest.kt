package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.controllers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE__SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.OffenderTransaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.time.OffsetDateTime
import java.util.UUID

class SyncOffenderTransactionTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient
      .post()
      .uri("/sync/offender-transactions")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    webTestClient
      .post()
      .uri("/sync/offender-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("SOME_OTHER_ROLE")))
      .bodyValue(createSyncOffenderTransactionRequest())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `201 Created - when transaction is genuinely new`() {
    val newTransactionRequest = createSyncOffenderTransactionRequest()

    webTestClient
      .post()
      .uri("/sync/offender-transactions")
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
    val invalidJson = """
        {
          "transactionId": 1234,          
          "caseloadId": "GMI",
          "transactionTimestamp": "${OffsetDateTime.now()}",
          "createdAt": "${OffsetDateTime.now()}",
          "createdBy": "TESTUSER",
          "createdByDisplayName": "Test User",
          "offenderTransactions": []
        }
    """.trimIndent()

    webTestClient
      .post()
      .uri("/sync/offender-transactions")
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
    val request = createSyncOffenderTransactionRequest().copy(createdBy = longString)

    webTestClient
      .post()
      .uri("/sync/offender-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE__SYNC)))
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Validation failure")
      .jsonPath("$.developerMessage").isEqualTo("Validation failed: createdBy: Created by must be supplied and be <= 32 characters")
  }

  private fun createSyncOffenderTransactionRequest(): SyncOffenderTransactionRequest = SyncOffenderTransactionRequest(
    transactionId = (1..Int.MAX_VALUE).random(),
    requestId = UUID.randomUUID(),
    caseloadId = "GMI",
    transactionTimestamp = OffsetDateTime.now(),
    createdAt = OffsetDateTime.now().minusHours(1),
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
}
