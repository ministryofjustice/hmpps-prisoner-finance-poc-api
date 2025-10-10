package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.migration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.GeneralLedgerBalancesSyncRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.GeneralLedgerPointInTimeBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.OffenderTransaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class MigrateInitialBalanceAndRecordTransactionTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `should migrate initial balance then sync offender transaction and verify balances`() {
    val migrateTimestamp = LocalDateTime.of(2025, 6, 1, 0, 0, 0) // Migration occurs first
    val transactionTimestamp = LocalDateTime.of(2025, 6, 2, 0, 8, 17) // Transaction occurs later

    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val prisonAccountCode = 1501
    val initialBalance = BigDecimal("-123.45")

    val migrationRequestBody = GeneralLedgerBalancesSyncRequest(
      accountBalances = listOf(
        GeneralLedgerPointInTimeBalance(accountCode = prisonAccountCode, balance = initialBalance, asOfTimestamp = migrateTimestamp),
      ),
    )

    webTestClient
      .post()
      .uri("/migrate/general-ledger-balances/{prisonId}", prisonId)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(migrationRequestBody))
      .exchange()
      .expectStatus().isOk

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, prisonAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(initialBalance.toDouble())

    val prisonNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()
    val transactionAmount = BigDecimal("1.25")
    val offenderAccountCode = 2102

    val transactionRequest = SyncOffenderTransactionRequest(
      transactionId = Random.nextLong(),
      caseloadId = prisonId,
      transactionTimestamp = transactionTimestamp,
      createdAt = transactionTimestamp.plusNanos(830000000),
      createdBy = "OMS_OWNER",
      offenderTransactions = listOf(
        OffenderTransaction(
          entrySequence = 1,
          offenderId = 5306470,
          offenderDisplayId = prisonNumber,
          offenderBookingId = 2970777,
          subAccountType = "SPND",
          postingType = "CR",
          type = "A_EARN",
          description = "Offender Payroll From:01/06/2025 To:01/06/2025",
          amount = transactionAmount.toDouble(),
          reference = null,
          generalLedgerEntries = listOf(
            GeneralLedgerEntry(entrySequence = 1, code = prisonAccountCode, postingType = "DR", amount = transactionAmount.toDouble()),
            GeneralLedgerEntry(entrySequence = 2, code = offenderAccountCode, postingType = "CR", amount = transactionAmount.toDouble()),
          ),
        ),
      ),
      requestId = UUID.randomUUID(),
      createdByDisplayName = "OMS_OWNER3",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
    )

    webTestClient
      .post()
      .uri("/sync/offender-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .bodyValue(objectMapper.writeValueAsString(transactionRequest))
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.action").isEqualTo("CREATED")

    val expectedFinalBalance = initialBalance.add(transactionAmount)

    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, prisonAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(expectedFinalBalance.toDouble())

    webTestClient
      .get()
      .uri("/prisoners/{prisonNumber}/accounts/{offenderAccountCode}", prisonNumber, offenderAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(transactionAmount.toDouble())
  }
}
