package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.migration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalancesRequest
import java.math.BigDecimal
import java.util.UUID

class MigrateGeneralLedgerBalancesTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `should migrate initial balances for a new prison and retrieve them correctly`() {
    // Arrange
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val accountCode1 = 2101
    val balance1 = BigDecimal("10000.50")
    val accountCode2 = 1101
    val balance2 = BigDecimal("-500.25")

    val requestBody = InitialGeneralLedgerBalancesRequest(
      initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = accountCode1, balance = balance1),
        InitialGeneralLedgerBalance(accountCode = accountCode2, balance = balance2),
      ),
    )

    // Act: POST request to migrate balances
    webTestClient
      .post()
      .uri("/migrate/general-ledger-balances/{prisonId}", prisonId)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(requestBody))
      .exchange()
      .expectStatus().isOk

    // Assert: GET request to retrieve the first account balance
    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, accountCode1)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(balance1.toDouble())
      .jsonPath("$.code").isEqualTo(accountCode1)
      .jsonPath("$.name").isEqualTo("Private Cash")

    // Assert: GET request to retrieve the second account balance
    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, accountCode2)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(balance2.toDouble())
      .jsonPath("$.code").isEqualTo(accountCode2)
      .jsonPath("$.name").isEqualTo("Bank")
  }
}
