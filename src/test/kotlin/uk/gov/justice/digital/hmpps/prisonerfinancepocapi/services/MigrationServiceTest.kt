package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Account
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.AccountType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialGeneralLedgerBalancesRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialPrisonerBalance
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.InitialPrisonerBalancesRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.AccountService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.PrisonService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.migration.MigrationService
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MigrationServiceTest {

  @Mock
  private lateinit var prisonService: PrisonService

  @Mock
  private lateinit var accountService: AccountService

  @Mock
  private lateinit var accountRepository: AccountRepository

  @InjectMocks
  private lateinit var migrationService: MigrationService

  private val prisonId = "ABC"
  private val prisonNumber = "A1234BC"
  private val prison = Prison(id = 1L, code = prisonId)

  private fun createGeneralLedgerAccount(id: Long, prisonId: Long, accountCode: Int, postingType: PostingType): Account = Account(
    id = id,
    uuid = UUID.randomUUID(),
    prisonId = prisonId,
    name = "GL Account",
    accountType = AccountType.GENERAL_LEDGER,
    accountCode = accountCode,
    initialDebits = BigDecimal.ZERO,
    initialCredits = BigDecimal.ZERO,
    totalDebits = BigDecimal.ZERO,
    totalCredits = BigDecimal.ZERO,
    initialOnHold = BigDecimal.ZERO,
    totalOnHold = BigDecimal.ZERO,
    postingType = postingType,
  )

  private fun createPrisonerAccount(id: Long, prisonId: Long, prisonNumber: String, accountCode: Int, subAccountType: String): Account = Account(
    id = id,
    uuid = UUID.randomUUID(),
    prisonId = prisonId,
    name = "$subAccountType Account",
    accountType = AccountType.PRISONER,
    accountCode = accountCode,
    prisonNumber = prisonNumber,
    subAccountType = subAccountType,
    initialDebits = BigDecimal.ZERO,
    initialCredits = BigDecimal.ZERO,
    totalDebits = BigDecimal.ZERO,
    totalCredits = BigDecimal.ZERO,
    initialOnHold = BigDecimal.ZERO,
    totalOnHold = BigDecimal.ZERO,
    postingType = PostingType.CR,
  )

  @Nested
  @DisplayName("migrateGeneralLedgerBalances")
  inner class MigrateGeneralLedgerBalancesTests {

    @Test
    fun `should create new prison and general ledger accounts if none exist`() {
      // Given
      val initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = 1000, balance = BigDecimal("100.50")),
        InitialGeneralLedgerBalance(accountCode = 2000, balance = BigDecimal("200.75")),
      )
      val request = InitialGeneralLedgerBalancesRequest(initialBalances = initialBalances)

      `when`(prisonService.getPrison(prisonId)).thenReturn(null)
      `when`(prisonService.createPrison(prisonId)).thenReturn(prison)
      `when`(accountService.findGeneralLedgerAccount(any(), any())).thenReturn(null)

      // When
      migrationService.migrateGeneralLedgerBalances(prisonId, request)

      // Then
      verify(prisonService).getPrison(prisonId)
      verify(prisonService).createPrison(prisonId)
      verify(accountService, times(2)).findGeneralLedgerAccount(eq(1L), any())
      verify(accountService).createGeneralLedgerAccountWithBalance(eq(1L), eq(1000), eq(BigDecimal("100.50")))
      verify(accountService).createGeneralLedgerAccountWithBalance(eq(1L), eq(2000), eq(BigDecimal("200.75")))
      verify(accountRepository, never()).save(any())
    }

    @Test
    fun `should update initial balances for existing general ledger accounts`() {
      // Given
      val existingAccount1 = createGeneralLedgerAccount(id = 10L, prisonId = 1L, accountCode = 1000, postingType = PostingType.DR)
      val existingAccount2 = createGeneralLedgerAccount(id = 11L, prisonId = 1L, accountCode = 2000, postingType = PostingType.CR)
      val initialBalances = listOf(
        InitialGeneralLedgerBalance(accountCode = 1000, balance = BigDecimal("50.00")),
        InitialGeneralLedgerBalance(accountCode = 2000, balance = BigDecimal("75.00")),
      )
      val request = InitialGeneralLedgerBalancesRequest(initialBalances = initialBalances)

      `when`(prisonService.getPrison(prisonId)).thenReturn(prison)
      `when`(accountService.findGeneralLedgerAccount(1L, 1000)).thenReturn(existingAccount1)
      `when`(accountService.findGeneralLedgerAccount(1L, 2000)).thenReturn(existingAccount2)

      // When
      migrationService.migrateGeneralLedgerBalances(prisonId, request)

      // Then
      verify(prisonService).getPrison(prisonId)
      verify(accountService, times(2)).findGeneralLedgerAccount(eq(1L), any())

      // Verify that the private helper method was called implicitly and the save was made
      verify(accountRepository, times(1)).save(existingAccount1)
      verify(accountRepository, times(1)).save(existingAccount2)

      // Verify the final state of the accounts
      assert(existingAccount1.initialDebits == BigDecimal("50.00"))
      assert(existingAccount1.initialCredits == BigDecimal.ZERO)
      assert(existingAccount1.totalDebits == BigDecimal.ZERO)

      assert(existingAccount2.initialCredits == BigDecimal("75.00"))
      assert(existingAccount2.initialDebits == BigDecimal.ZERO)
      assert(existingAccount2.totalCredits == BigDecimal.ZERO)

      verify(accountService, never()).createGeneralLedgerAccountWithBalance(any(), any(), any())
    }
  }

  @Nested
  @DisplayName("migratePrisonerBalances")
  inner class MigratePrisonerBalancesTests {

    @Test
    fun `should create new prisoner accounts if they don't exist`() {
      // Given
      val initialBalances = listOf(
        InitialPrisonerBalance(accountCode = 1000, balance = BigDecimal("10.00"), holdBalance = BigDecimal("5.00")),
        InitialPrisonerBalance(accountCode = 2000, balance = BigDecimal("20.00"), holdBalance = BigDecimal.ZERO),
      )
      val request = InitialPrisonerBalancesRequest(prisonId = prisonId, initialBalances = initialBalances)

      `when`(prisonService.getPrison(prisonId)).thenReturn(prison)
      `when`(accountService.findPrisonerAccount(any(), any())).thenReturn(null)

      // When
      migrationService.migratePrisonerBalances(prisonNumber, request)

      // Then
      verify(prisonService).getPrison(prisonId)
      verify(accountService, times(2)).findPrisonerAccount(eq(prisonNumber), any())
      // Use named arguments to match the order in MigrationService
      verify(accountService).createPrisonerAccountWithBalance(
        prisonNumber = eq(prisonNumber),
        accountCode = eq(1000),
        initialBalance = eq(BigDecimal("10.00")),
        prisonId = eq(1L),
        initialOnHold = eq(BigDecimal("5.00")),
      )
      verify(accountService).createPrisonerAccountWithBalance(
        prisonNumber = eq(prisonNumber),
        accountCode = eq(2000),
        initialBalance = eq(BigDecimal("20.00")),
        prisonId = eq(1L),
        initialOnHold = eq(BigDecimal.ZERO),
      )
      verify(accountRepository, never()).save(any())
    }

    @Test
    fun `should initialize balances and hold balances for existing prisoner accounts`() {
      // Given
      val existingAccount1 = createPrisonerAccount(id = 20L, prisonId = 1L, prisonNumber = prisonNumber, accountCode = 1000, subAccountType = "Spends")
      val existingAccount2 = createPrisonerAccount(id = 21L, prisonId = 1L, prisonNumber = prisonNumber, accountCode = 2000, subAccountType = "Savings")
      val initialBalances = listOf(
        InitialPrisonerBalance(accountCode = 1000, balance = BigDecimal("35.00"), holdBalance = BigDecimal("10.00")),
        InitialPrisonerBalance(accountCode = 2000, balance = BigDecimal("45.00"), holdBalance = BigDecimal("5.00")),
      )
      val request = InitialPrisonerBalancesRequest(prisonId = prisonId, initialBalances = initialBalances)

      `when`(prisonService.getPrison(prisonId)).thenReturn(prison)
      `when`(accountService.findPrisonerAccount(prisonNumber, 1000)).thenReturn(existingAccount1)
      `when`(accountService.findPrisonerAccount(prisonNumber, 2000)).thenReturn(existingAccount2)

      // When
      migrationService.migratePrisonerBalances(prisonNumber, request)

      // Then
      verify(prisonService).getPrison(prisonId)
      verify(accountService, times(2)).findPrisonerAccount(eq(prisonNumber), any())

      // Assert that balances and hold balances were correctly initialized
      verify(accountRepository, times(1)).save(existingAccount1)
      verify(accountRepository, times(1)).save(existingAccount2)

      assert(existingAccount1.initialCredits == BigDecimal("35.00"))
      assert(existingAccount1.totalCredits == BigDecimal.ZERO)
      assert(existingAccount1.initialOnHold == BigDecimal("10.00"))
      assert(existingAccount1.totalOnHold == BigDecimal.ZERO)

      assert(existingAccount2.initialCredits == BigDecimal("45.00"))
      assert(existingAccount2.totalCredits == BigDecimal.ZERO)
      assert(existingAccount2.initialOnHold == BigDecimal("5.00"))
      assert(existingAccount2.totalOnHold == BigDecimal.ZERO)

      verify(accountService, never()).createPrisonerAccountWithBalance(any(), any(), any(), any(), any())
    }

    @Test
    fun `should throw EntityNotFoundException if prison is not found for prisoner migration`() {
      // Given
      val initialBalances = listOf(InitialPrisonerBalance(accountCode = 1000, balance = BigDecimal("50.00"), holdBalance = BigDecimal("10.00")))
      val request = InitialPrisonerBalancesRequest(prisonId = prisonId, initialBalances = initialBalances)

      `when`(prisonService.getPrison(prisonId)).thenReturn(null)

      // When / Then
      assertThrows<EntityNotFoundException> {
        migrationService.migratePrisonerBalances(prisonNumber, request)
      }
      verify(prisonService).getPrison(prisonId)
      verify(accountService, never()).findPrisonerAccount(any(), any())
    }
  }
}
