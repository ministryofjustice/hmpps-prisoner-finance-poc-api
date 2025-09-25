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

class MigratePrisonerWithHoldRemovalTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `should migrate initial balances including holds and remove a hold correctly`() {
    // Arrange: Use unique identifiers for test isolation
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val prisonNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()

    val privateCashAccountCode = 2101
    val holdsGLAccountCode = 2199

    val initialCashBalance = BigDecimal("32.00")
    val initialCashHoldBalance = BigDecimal("8.00")
    val removalAmount = BigDecimal("8.00")

    // Step 1: Set up the initial prison accounts for the migration
    val glMigrationRequestBody = InitialGeneralLedgerBalancesRequest(
      initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = holdsGLAccountCode, balance = initialCashHoldBalance),
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

    // Step 2: Use the prisoner migration endpoint to create the prisoner accounts
    val prisonerMigrationRequestBody = InitialPrisonerBalancesRequest(
      prisonId = prisonId,
      initialBalances = listOf(
        InitialPrisonerBalance(
          accountCode = privateCashAccountCode,
          balance = initialCashBalance,
          holdBalance = initialCashHoldBalance,
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

    // Step 3: Verify the migrated balances for the prisoner's private cash account
    webTestClient
      .get()
      .uri("/prisoners/{prisonNumber}/accounts/{accountCode}", prisonNumber, privateCashAccountCode)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.balance").isEqualTo(initialCashBalance.toDouble())
      .jsonPath("$.holdBalance").isEqualTo(initialCashHoldBalance.toDouble())

    // Step 4: Remove the hold via a regular transaction
    val removeHoldRequest = SyncOffenderTransactionRequest(
      transactionId = Random.nextLong(),
      caseloadId = prisonId,
      transactionTimestamp = LocalDateTime.of(2025, 9, 19, 10, 0, 0),
      createdAt = LocalDateTime.of(2025, 9, 19, 10, 0, 0, 0),
      createdBy = "SOME_USER",
      offenderTransactions = listOf(
        OffenderTransaction(
          entrySequence = 1,
          offenderId = 2609628,
          offenderDisplayId = prisonNumber,
          offenderBookingId = 1227181,
          subAccountType = "REG",
          postingType = "CR",
          type = "HOR",
          description = "HOLD RELEASE",
          amount = removalAmount.toDouble(),
          reference = null,
          generalLedgerEntries = listOf(
            GeneralLedgerEntry(entrySequence = 1, code = holdsGLAccountCode, postingType = "DR", amount = removalAmount.toDouble()),
            GeneralLedgerEntry(entrySequence = 2, code = privateCashAccountCode, postingType = "CR", amount = removalAmount.toDouble()),
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
      .bodyValue(objectMapper.writeValueAsString(removeHoldRequest))
      .exchange()
      .expectStatus().isCreated

    // Step 5: Verify the final balances after the hold is removed
    val expectedFinalAvailableBalance = initialCashBalance.add(removalAmount)
    val expectedFinalHoldBalance = initialCashHoldBalance.subtract(removalAmount)

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
