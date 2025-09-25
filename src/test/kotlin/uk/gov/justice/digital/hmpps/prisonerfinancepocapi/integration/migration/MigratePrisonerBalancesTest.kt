package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.migration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalancesRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialPrisonerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialPrisonerBalancesRequest
import java.math.BigDecimal
import java.util.UUID

class MigratePrisonerBalancesTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `should migrate initial balances for a new prisoner and retrieve them correctly`() {
    // Arrange: Use unique identifiers for test isolation
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val prisonNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()

    val spendsAccountCode = 2102
    val spendsBalance = BigDecimal("50.00")
    val savingsAccountCode = 2103
    val savingsBalance = BigDecimal("123.45")

    // Step 1: Use the GL migration endpoint to set up the prison accounts
    val glMigrationRequestBody = InitialGeneralLedgerBalancesRequest(
      initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = spendsAccountCode, balance = spendsBalance),
        InitialGeneralLedgerBalance(accountCode = savingsAccountCode, balance = savingsBalance),
      ),
    )

    webTestClient
      .post()
      .uri("/migrate/general-ledger-balances/{prisonId}", prisonId)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(glMigrationRequestBody))
      .exchange()
      .expectStatus().isOk

    // Verify GL accounts were set correctly after GL migration
    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, spendsAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(spendsBalance.toDouble())

    // Step 2: Now, use the prisoner migration endpoint to create the prisoner accounts
    val prisonerMigrationRequestBody = InitialPrisonerBalancesRequest(
      prisonId = prisonId,
      initialBalances = listOf(
        // The fix is here. Use InitialPrisonerBalance instead of InitialGeneralLedgerBalance
        InitialPrisonerBalance(accountCode = spendsAccountCode, balance = spendsBalance, holdBalance = BigDecimal.ZERO),
        InitialPrisonerBalance(accountCode = savingsAccountCode, balance = savingsBalance, holdBalance = BigDecimal.ZERO),
      ),
    )

    webTestClient
      .post()
      .uri("/migrate/prisoner-balances/{prisonNumber}", prisonNumber)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(prisonerMigrationRequestBody))
      .exchange()
      .expectStatus().isOk

    // Assert: Get request to retrieve the prisoner's spends balance
    webTestClient
      .get()
      .uri("/prisoners/{prisonNumber}/accounts/{accountCode}", prisonNumber, spendsAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(spendsBalance.toDouble())
      .jsonPath("$.code").isEqualTo(spendsAccountCode)
      .jsonPath("$.name").isEqualTo("Spends")
      // Also, assert the hold balance is zero
      .jsonPath("$.holdBalance").isEqualTo(BigDecimal.ZERO.toDouble())

    // Assert: Get request to retrieve the prisoner's savings balance
    webTestClient
      .get()
      .uri("/prisoners/{prisonNumber}/accounts/{accountCode}", prisonNumber, savingsAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(savingsBalance.toDouble())
      .jsonPath("$.code").isEqualTo(savingsAccountCode)
      .jsonPath("$.name").isEqualTo("Savings")
      // Also, assert the hold balance is zero
      .jsonPath("$.holdBalance").isEqualTo(BigDecimal.ZERO.toDouble())

    // Assert: Verify the general ledger accounts reflect the new liabilities
    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, spendsAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(spendsBalance.toDouble())
      .jsonPath("$.name").isEqualTo("Spends")

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, savingsAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(savingsBalance.toDouble())
      .jsonPath("$.name").isEqualTo("Savings")
  }
}
