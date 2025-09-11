package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.ROLE_PRISONER_FINANCE_SYNC
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.PrisonAccountDetails
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalancesRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.GeneralLedgerEntry
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class SyncGeneralLedgerTransactionBalanceIntegrationTest : IntegrationTestBase() {

  // --- Test 1: Debit to a DR (Asset) account increases its balance ---
  @Test
  fun `Given a debit posting to an asset account, the balance increases as expected`() {
    // Define test data
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val debitAccountCode = 1501 // Asset account, DR posting type
    val creditAccountCode = 2501 // Liability account, CR posting type
    val transactionAmount = BigDecimal("45.00")
    val initialDebitBalance = BigDecimal("100.00")
    val initialCreditBalance = BigDecimal("200.00")

    // A debit posting to an Asset (DR) account increases its balance.
    val expectedFinalDebitBalance = initialDebitBalance.add(transactionAmount)
    // A credit posting to a Liability (CR) account increases its balance.
    val expectedFinalCreditBalance = initialCreditBalance.add(transactionAmount)

    // Run the transaction and verify balances
    executeGlTransactionAndVerifyBalances(
      prisonId = prisonId,
      debitAccountCode = debitAccountCode,
      creditAccountCode = creditAccountCode,
      transactionAmount = transactionAmount,
      initialDebitBalance = initialDebitBalance,
      initialCreditBalance = initialCreditBalance,
      expectedFinalDebitBalance = expectedFinalDebitBalance,
      expectedFinalCreditBalance = expectedFinalCreditBalance,
      debitPostingType = "DR",
      creditPostingType = "CR",
    )
  }

  // --- Test 2: Credit to a CR (Liability) account increases its balance ---
  @Test
  fun `Given a credit posting to a liability account, the balance increases as expected`() {
    // Define test data
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val debitAccountCode = 4102 // Disbursement account, DR posting type
    val creditAccountCode = 2501 // Liability account, CR posting type
    val transactionAmount = BigDecimal("75.50")
    val initialDebitBalance = BigDecimal("500.00")
    val initialCreditBalance = BigDecimal("100.00")

    // A debit posting to a Disbursement (DR) account increases its balance.
    val expectedFinalDebitBalance = initialDebitBalance.add(transactionAmount)
    // A credit posting to a Liability (CR) account increases its balance.
    val expectedFinalCreditBalance = initialCreditBalance.add(transactionAmount)

    // Run the transaction and verify balances
    executeGlTransactionAndVerifyBalances(
      prisonId = prisonId,
      debitAccountCode = debitAccountCode,
      creditAccountCode = creditAccountCode,
      transactionAmount = transactionAmount,
      initialDebitBalance = initialDebitBalance,
      initialCreditBalance = initialCreditBalance,
      expectedFinalDebitBalance = expectedFinalDebitBalance,
      expectedFinalCreditBalance = expectedFinalCreditBalance,
      debitPostingType = "DR",
      creditPostingType = "CR",
    )
  }

  // --- Test 3: Debit to a CR (Liability) account decreases its balance ---
  @Test
  fun `Given a debit posting to a liability account, the balance decreases as expected`() {
    // Define test data
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val debitAccountCode = 2501 // Liability account, CR posting type
    val creditAccountCode = 1101 // Asset account, DR posting type
    val transactionAmount = BigDecimal("25.00")
    val initialDebitBalance = BigDecimal("200.00")
    val initialCreditBalance = BigDecimal("500.00")

    // A debit posting to a Liability (CR) account decreases its balance.
    val expectedFinalDebitBalance = initialDebitBalance.subtract(transactionAmount)
    // A credit posting to an Asset (DR) account decreases its balance.
    val expectedFinalCreditBalance = initialCreditBalance.subtract(transactionAmount)

    // Run the transaction and verify balances
    executeGlTransactionAndVerifyBalances(
      prisonId = prisonId,
      debitAccountCode = debitAccountCode,
      creditAccountCode = creditAccountCode,
      transactionAmount = transactionAmount,
      initialDebitBalance = initialDebitBalance,
      initialCreditBalance = initialCreditBalance,
      expectedFinalDebitBalance = expectedFinalDebitBalance,
      expectedFinalCreditBalance = expectedFinalCreditBalance,
      debitPostingType = "DR",
      creditPostingType = "CR",
    )
  }

  // --- Test 4: Credit to a DR (Asset) account decreases its balance ---
  @Test
  fun `Given a credit posting to an asset account, the balance decreases as expected`() {
    // Define test data
    val prisonId = UUID.randomUUID().toString().substring(0, 3).uppercase()
    val debitAccountCode = 2501 // This is a Liability account with CR posting type
    val creditAccountCode = 1101 // This is an Asset account with DR posting type
    val transactionAmount = BigDecimal("15.00")
    val initialDebitBalance = BigDecimal("300.00")
    val initialCreditBalance = BigDecimal("100.00")

    // A debit posting to a Liability (CR) account decreases its balance.
    val expectedFinalDebitBalance = initialDebitBalance.subtract(transactionAmount)
    // A credit posting to an Asset (DR) account decreases its balance.
    val expectedFinalCreditBalance = initialCreditBalance.subtract(transactionAmount)

    // Run the transaction and verify balances
    executeGlTransactionAndVerifyBalances(
      prisonId = prisonId,
      debitAccountCode = debitAccountCode,
      creditAccountCode = creditAccountCode,
      transactionAmount = transactionAmount,
      initialDebitBalance = initialDebitBalance,
      initialCreditBalance = initialCreditBalance,
      expectedFinalDebitBalance = expectedFinalDebitBalance,
      expectedFinalCreditBalance = expectedFinalCreditBalance,
      debitPostingType = "DR",
      creditPostingType = "CR",
    )
  }

  private fun executeGlTransactionAndVerifyBalances(
    prisonId: String,
    debitAccountCode: Int,
    creditAccountCode: Int,
    transactionAmount: BigDecimal,
    initialDebitBalance: BigDecimal,
    initialCreditBalance: BigDecimal,
    expectedFinalDebitBalance: BigDecimal,
    expectedFinalCreditBalance: BigDecimal,
    debitPostingType: String,
    creditPostingType: String,
  ) {
    // Step 1: Migrate the initial GL balances
    val initialBalancesRequest = InitialGeneralLedgerBalancesRequest(
      initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = debitAccountCode, balance = initialDebitBalance),
        InitialGeneralLedgerBalance(accountCode = creditAccountCode, balance = initialCreditBalance),
      ),
    )

    webTestClient
      .post()
      .uri("/migrate/general-ledger-balances/{prisonId}", prisonId)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(initialBalancesRequest)
      .exchange()
      .expectStatus().isOk

    // Step 2: Post a new, balanced transaction with an idempotent requestId and transactionId
    val glTransactionRequest = SyncGeneralLedgerTransactionRequest(
      transactionId = Random.nextLong(),
      requestId = UUID.randomUUID(),
      description = "Test GL Transaction",
      caseloadId = prisonId,
      transactionType = "GJ",
      generalLedgerEntries = listOf(
        GeneralLedgerEntry(entrySequence = 1, code = debitAccountCode, postingType = debitPostingType, amount = transactionAmount.toDouble()),
        GeneralLedgerEntry(entrySequence = 2, code = creditAccountCode, postingType = creditPostingType, amount = transactionAmount.toDouble()),
      ),
      transactionTimestamp = LocalDateTime.of(2025, 9, 18, 8, 32, 53),
      createdAt = LocalDateTime.of(2025, 9, 18, 8, 32, 53),
      createdBy = "SOMEONE",
      createdByDisplayName = "SOMEONE",
      reference = null, lastModifiedAt = null, lastModifiedBy = null, lastModifiedByDisplayName = null,
    )

    webTestClient
      .post()
      .uri("/sync/general-ledger-transactions")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
      .bodyValue(glTransactionRequest)
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.action").isEqualTo("CREATED")

    // Step 3: Verify the final balances
    val finalDebitBalance = getAccountDetails(prisonId, debitAccountCode)?.balance
    val finalCreditBalance = getAccountDetails(prisonId, creditAccountCode)?.balance

    assertThat(finalDebitBalance).isEqualTo(expectedFinalDebitBalance)
    assertThat(finalCreditBalance).isEqualTo(expectedFinalCreditBalance)
  }

  // Helper function to get account details, including balance
  private fun getAccountDetails(prisonId: String, accountCode: Int): PrisonAccountDetails? = webTestClient
    .get()
    .uri("/prisons/$prisonId/accounts/$accountCode")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_FINANCE_SYNC)))
    .exchange()
    .expectBody(PrisonAccountDetails::class.java)
    .returnResult()
    .responseBody
}
