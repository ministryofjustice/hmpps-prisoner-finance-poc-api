package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.migration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.GeneralLedgerBalancesSyncRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.GeneralLedgerPointInTimeBalance
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class MigrateGeneralLedgerBalancesTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `should migrate initial balances for non-prisoner GL accounts correctly`() {
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val accountCode1 = 1501 // Receivable For Earnings (Asset)
    val balance1 = BigDecimal("10000.50")
    val accountCode2 = 2501 // Canteen Payable (Liability)
    val balance2 = BigDecimal("-500.25")

    val localDateTime1 = LocalDateTime.now().minusDays(1)
    val localDateTime2 = LocalDateTime.now()

    val expectedDate1 = localDateTime1.atZone(ZoneId.systemDefault()).toInstant().toString()
    val expectedDate2 = localDateTime2.atZone(ZoneId.systemDefault()).toInstant().toString()

    val requestBody = GeneralLedgerBalancesSyncRequest(
      accountBalances = listOf(
        GeneralLedgerPointInTimeBalance(accountCode = accountCode1, balance = balance1, asOfTimestamp = localDateTime1),
        GeneralLedgerPointInTimeBalance(accountCode = accountCode2, balance = balance2, asOfTimestamp = localDateTime2),
      ),
    )

    webTestClient
      .post()
      .uri("/migrate/general-ledger-balances/{prisonId}", prisonId)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(requestBody))
      .exchange()
      .expectStatus().isOk

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, accountCode1)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(balance1.toDouble())
      .jsonPath("$.code").isEqualTo(accountCode1)
      .jsonPath("$.name").isEqualTo("Receivable For Earnings")

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, accountCode2)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(balance2.toDouble())
      .jsonPath("$.code").isEqualTo(accountCode2)
      .jsonPath("$.name").isEqualTo("Canteen Payable")

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}/transactions", prisonId, accountCode1)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].date").isEqualTo(expectedDate1)

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}/transactions", prisonId, accountCode2)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.items.length()").isEqualTo(1)
      .jsonPath("$.items[0].date").isEqualTo(expectedDate2)
  }
}
