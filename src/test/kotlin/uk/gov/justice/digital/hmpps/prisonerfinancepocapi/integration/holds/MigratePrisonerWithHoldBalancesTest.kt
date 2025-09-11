package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.holds

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
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.OffenderTransaction
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class MigratePrisonerWithHoldBalancesTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `should migrate initial balances including holds and apply a new hold correctly`() {
    // Arrange: Use unique identifiers for test isolation
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val prisonNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()

    val privateCashAccountCode = 2101
    val initialAvailableBalance = BigDecimal("37.00")
    val initialHoldBalance = BigDecimal("3.00")
    val newHoldAmount = BigDecimal("5.00")
    val holdsGLAccountCode = 2199

    // Step 1: Set up the initial hold accounts for the migration
    val glMigrationRequestBody = InitialGeneralLedgerBalancesRequest(
      initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = holdsGLAccountCode, balance = initialHoldBalance),
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

    // Step 2: Now, use the prisoner migration endpoint to create the prisoner accounts, including the hold
    val prisonerMigrationRequestBody = InitialPrisonerBalancesRequest(
      prisonId = prisonId,
      initialBalances = listOf(
        InitialPrisonerBalance(
          accountCode = privateCashAccountCode,
          balance = initialAvailableBalance,
          holdBalance = initialHoldBalance,
        ),
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

    // Step 3: Verify the migrated balances for the prisoner
    webTestClient
      .get()
      .uri("/prisoners/{prisonNumber}/accounts/{accountCode}", prisonNumber, privateCashAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(initialAvailableBalance.toDouble())
      .jsonPath("$.holdBalance").isEqualTo(initialHoldBalance.toDouble())

    // Step 4: Add a new hold via a regular transaction
    val addHoldRequest = SyncOffenderTransactionRequest(
      transactionId = Random.nextLong(),
      caseloadId = prisonId,
      transactionTimestamp = LocalDateTime.of(2025, 9, 18, 16, 57, 11),
      createdAt = LocalDateTime.of(2025, 9, 18, 16, 57, 11, 971000000),
      createdBy = "SOME_USER",
      offenderTransactions = listOf(
        OffenderTransaction(
          entrySequence = 1,
          offenderId = 2609628,
          offenderDisplayId = prisonNumber,
          offenderBookingId = 1227181,
          subAccountType = "REG",
          postingType = "DR",
          type = "HOA",
          description = "HOLD",
          amount = newHoldAmount.toDouble(),
          reference = null,
          generalLedgerEntries = listOf(
            GeneralLedgerEntry(entrySequence = 1, code = privateCashAccountCode, postingType = "DR", amount = newHoldAmount.toDouble()),
            GeneralLedgerEntry(entrySequence = 2, code = holdsGLAccountCode, postingType = "CR", amount = newHoldAmount.toDouble()),
          ),
        ),
      ),
      requestId = UUID.randomUUID(),
      createdByDisplayName = "SOME_USER",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
    )

    webTestClient
      .post()
      .uri("/sync/offender-transactions")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(objectMapper.writeValueAsString(addHoldRequest))
      .exchange()
      .expectStatus().isCreated

    // Step 5: Verify the final balances after the new hold is applied
    val expectedFinalAvailableBalance = initialAvailableBalance.subtract(newHoldAmount)
    val expectedFinalHoldBalance = initialHoldBalance.add(newHoldAmount)

    webTestClient
      .get()
      .uri("/prisoners/{prisonNumber}/accounts/{accountCode}", prisonNumber, privateCashAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(expectedFinalAvailableBalance.toDouble())
      .jsonPath("$.holdBalance").isEqualTo(expectedFinalHoldBalance.toDouble())
  }
}
