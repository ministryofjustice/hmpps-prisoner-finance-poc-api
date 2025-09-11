package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.migration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalancesRequest
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
  fun `should_migrate_initial_balance_then_sync_offender_transaction_and_verify_balances`() {
    // Arrange: Data for initial balance migration
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val prisonAccountCode = 1501
    val initialBalance = BigDecimal("-123.45")

    val migrationRequestBody = InitialGeneralLedgerBalancesRequest(
      initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = prisonAccountCode, balance = initialBalance),
      ),
    )

    // Step 1: Migrate the initial balance for the General Ledger account
    webTestClient
      .post()
      .uri("/migrate/general-ledger-balances/{prisonId}", prisonId)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(migrationRequestBody))
      .exchange()
      .expectStatus().isOk

    // Verify the initial balance was set correctly
    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, prisonAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(initialBalance.toDouble())

    // Arrange: Data for a new offender transaction
    val prisonNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()
    val transactionAmount = BigDecimal("1.25")
    val offenderAccountCode = 2102

    val transactionRequest = SyncOffenderTransactionRequest(
      transactionId = Random.nextLong(),
      caseloadId = prisonId,
      transactionTimestamp = LocalDateTime.of(2025, 6, 2, 0, 8, 17),
      createdAt = LocalDateTime.of(2025, 6, 2, 0, 8, 17, 830000000),
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

    // Step 2: Sync a new offender transaction
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

    // Calculate the expected final balance
    val expectedFinalBalance = initialBalance.add(transactionAmount)

    // Step 3: Verify the final balance of the General Ledger account
    webTestClient
      .get()
      .uri("/prisons/{prisonId}/accounts/{accountCode}", prisonId, prisonAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(expectedFinalBalance.toDouble())

    // Verify the offender's account balance
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
