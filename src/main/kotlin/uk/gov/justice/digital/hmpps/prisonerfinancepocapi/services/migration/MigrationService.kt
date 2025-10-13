package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.migration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.PostingType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.GeneralLedgerBalancesSyncRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.migration.PrisonerBalancesSyncRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.RequestCaptureService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.TimeConversionService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.AccountService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.PrisonService
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.ledger.TransactionService
import java.math.BigDecimal
import java.time.Instant

@Service
open class MigrationService(
  private val prisonService: PrisonService,
  private val accountService: AccountService,
  private val transactionService: TransactionService,
  private val timeConversionService: TimeConversionService,
  private val requestCaptureService: RequestCaptureService,
) {

  @Transactional
  fun migrateGeneralLedgerBalances(prisonId: String, request: GeneralLedgerBalancesSyncRequest) {
    requestCaptureService.captureGeneralLedgerMigrationRequest(prisonId, request)

    val migrationBatchTime = Instant.now()

    val prisonerAccountCodes = listOf(2101, 2102, 2103)

    val prison = prisonService.getPrison(prisonId)
      ?: prisonService.createPrison(prisonId)

    val clearingAccount = accountService.findGeneralLedgerAccount(
      prisonId = prison.id!!,
      accountCode = 9999,
    ) ?: accountService.createGeneralLedgerAccount(
      prisonId = prison.id,
      accountCode = 9999,
    )

    request.accountBalances
      .filter { balanceData -> balanceData.accountCode !in prisonerAccountCodes }
      .forEach { balanceData ->

        val transactionTimestamp = timeConversionService.toUtcInstant(balanceData.asOfTimestamp)

        val account = accountService.findGeneralLedgerAccount(
          prisonId = prison.id,
          accountCode = balanceData.accountCode,
        ) ?: accountService.createGeneralLedgerAccount(
          prisonId = prison.id,
          accountCode = balanceData.accountCode,
        )

        val allEntries = mutableListOf<Triple<Long, BigDecimal, PostingType>>()
        var netBalance = BigDecimal.ZERO

        if (balanceData.balance.signum() != 0) {
          val entryType: PostingType
          when (account.postingType) {
            PostingType.DR -> {
              entryType = if (balanceData.balance.signum() >= 0) {
                netBalance += balanceData.balance
                PostingType.DR
              } else {
                netBalance += balanceData.balance
                PostingType.CR
              }
            }
            PostingType.CR -> {
              entryType = if (balanceData.balance.signum() >= 0) {
                netBalance -= balanceData.balance
                PostingType.CR
              } else {
                netBalance -= balanceData.balance
                PostingType.DR
              }
            }
          }
          allEntries.add(Triple(account.id!!, balanceData.balance.abs(), entryType))
        }

        if (netBalance.signum() != 0) {
          val balancingEntryType = if (netBalance.signum() > 0) PostingType.CR else PostingType.DR
          allEntries.add(Triple(clearingAccount.id!!, netBalance.abs(), balancingEntryType))
        }

        if (allEntries.isNotEmpty()) {
          transactionService.recordTransaction(
            transactionType = "OB",
            description = "General Ledger Point-in-Time Balance Sync",
            entries = allEntries,
            transactionTimestamp = transactionTimestamp,
            prison = prisonId,
            createdAt = migrationBatchTime,
          )
        }
      }
  }

  @Transactional
  fun migratePrisonerBalances(prisonNumber: String, request: PrisonerBalancesSyncRequest) {
    requestCaptureService.capturePrisonerMigrationRequest(prisonNumber, request)

    val migrationBatchTime = Instant.now()

    request.accountBalances.forEach { balanceData ->
      val prison = prisonService.getPrison(balanceData.prisonId)
        ?: prisonService.createPrison(balanceData.prisonId)
      val clearingAccount = accountService.findGeneralLedgerAccount(
        prisonId = prison.id!!,
        accountCode = 9999,
      ) ?: accountService.createGeneralLedgerAccount(
        prisonId = prison.id,
        accountCode = 9999,
      )

      val holdAccount = accountService.findGeneralLedgerAccount(
        prisonId = prison.id,
        accountCode = 2199,
      ) ?: accountService.createGeneralLedgerAccount(
        prisonId = prison.id,
        accountCode = 2199,
      )

      val prisonerAccount = accountService.resolveAccount(
        balanceData.accountCode,
        prisonNumber,
        prison.id,
      )

      val availableBalance = balanceData.balance
      val holdBalance = balanceData.holdBalance

      val transactionTimestamp = timeConversionService.toUtcInstant(balanceData.asOfTimestamp)

      if (availableBalance.signum() != 0) {
        val availableEntries = mutableListOf<Triple<Long, BigDecimal, PostingType>>()
        val entryType = if (availableBalance.signum() > 0) PostingType.CR else PostingType.DR
        availableEntries.add(Triple(prisonerAccount.id!!, availableBalance.abs(), entryType))
        availableEntries.add(Triple(clearingAccount.id!!, availableBalance.abs(), if (entryType == PostingType.CR) PostingType.DR else PostingType.CR))

        transactionService.recordTransaction(
          transactionType = "OB",
          description = "Prisoner Balance Migration",
          entries = availableEntries,
          transactionTimestamp = transactionTimestamp,
          prison = balanceData.prisonId,
          legacyTransactionId = balanceData.transactionId,
          createdAt = migrationBatchTime,
        )
      }

      if (holdBalance.signum() != 0) {
        val holdEntries = mutableListOf<Triple<Long, BigDecimal, PostingType>>()
        holdEntries.add(Triple(prisonerAccount.id!!, holdBalance.abs(), PostingType.DR))
        holdEntries.add(Triple(holdAccount.id!!, holdBalance.abs(), PostingType.CR))

        transactionService.recordTransaction(
          transactionType = "OHB",
          description = "Prisoner Hold Balance Migration",
          entries = holdEntries,
          transactionTimestamp = transactionTimestamp,
          prison = balanceData.prisonId,
          legacyTransactionId = balanceData.transactionId,
          createdAt = migrationBatchTime,
        )
      }
    }
  }
}
