package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE__SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerAccountBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerBalanceRequest
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SyncGeneralLedgerBalanceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `401 unauthorised`() {
    webTestClient
      .post()
      .uri("/sync/general-ledger-balances")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    webTestClient
      .post()
      .uri("/sync/general-ledger-balances")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("SOME_OTHER_ROLE")))
      .bodyValue(createSyncGeneralLedgerBalanceRequest())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `201 Created - when balance is new`() {
    val request = createSyncGeneralLedgerBalanceRequest()

    webTestClient
      .post()
      .uri("/sync/general-ledger-balances")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE__SYNC)))
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.requestId").isEqualTo(request.requestId.toString())
      .jsonPath("$.action").isEqualTo("CREATED")
  }

  @Test
  fun `400 Bad Request - missing required requestId`() {
    val invalidMap = mapOf(
      "timestamp" to OffsetDateTime.now(),
      "balances" to emptyList<Any>(),
    )
    val invalidJson = objectMapper.writeValueAsString(invalidMap)

    webTestClient
      .post()
      .uri("/sync/general-ledger-balances")
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
        assertThat(message).contains("requestId")
      }
  }

  @Test
  fun `400 Bad Request - missing balances`() {
    val invalidMap = mapOf(
      "requestId" to UUID.randomUUID(),
      "timestamp" to OffsetDateTime.now(),
    )
    val invalidJson = objectMapper.writeValueAsString(invalidMap)

    webTestClient
      .post()
      .uri("/sync/general-ledger-balances")
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
        assertThat(message).contains("balances")
      }
  }

  private fun createSyncGeneralLedgerBalanceRequest(): SyncGeneralLedgerBalanceRequest = SyncGeneralLedgerBalanceRequest(
    requestId = UUID.randomUUID(),
    timestamp = OffsetDateTime.now(),
    balances = listOf(
      GeneralLedgerAccountBalance(code = 1101, name = "Bank", balance = BigDecimal("12.50")),
    ),
  )
}
