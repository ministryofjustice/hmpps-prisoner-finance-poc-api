package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.reports

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionType
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.AccountRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionEntryRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.TransactionTypeRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.reports.SummaryOfPaymentAndReceiptsReport
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class ReportService(
  private val accountRepository: AccountRepository,
  private val transactionRepository: TransactionRepository,
  private val transactionEntryRepository: TransactionEntryRepository,
  private val transactionTypeRepository: TransactionTypeRepository,
) {

  fun generateDailyPrisonSummaryReport(
    prisonId: String,
    date: LocalDate,
  ): List<SummaryOfPaymentAndReceiptsReport.PostingReportEntry> {
    // 1. Define the date range for a single business day
    val dateStart = date.atStartOfDay(ZoneOffset.UTC)
    val dateEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC)

    // 2. Fetch all transaction entries for the specified date, filtering by transaction.prison code.
    val dailyTransactionEntries = transactionEntryRepository.findByDateBetweenAndPrisonCode(
      Timestamp.from(dateStart.toInstant()),
      Timestamp.from(dateEnd.toInstant()),
      prisonId, // Use the prison CODE for filtering on the transaction table
    )

    // 3. Return an empty list if no transactions occurred on this day
    if (dailyTransactionEntries.isEmpty()) {
      return emptyList()
    }

    // 4. Fetch related transactions and transaction types in a single batch.
    val transactionIds = dailyTransactionEntries.map { it.transactionId }.distinct()
    val transactions = transactionRepository.findAllById(transactionIds).associateBy { it.id }

    val accountIds = dailyTransactionEntries.map { it.accountId }.distinct()
    val prisonAccounts = accountRepository.findAllById(accountIds).associateBy { it.id }

    val transactionTypeNames = transactions.values.map { it.transactionType }.distinct()
    val transactionTypes = transactionTypeRepository.findByTxnTypeIn(transactionTypeNames).associateBy { it.txnType }

    // 5. Transform and aggregate the data into report entries.
    return dailyTransactionEntries
      .mapNotNull { entry ->
        val transaction = transactions[entry.transactionId]
        val account = prisonAccounts[entry.accountId]

        // Skip entries with missing or corrupted data.
        if (transaction == null || account == null) {
          return@mapNotNull null
        }

        // Create a temporary report entry to hold values for a single transaction.
        val tempPosting = SummaryOfPaymentAndReceiptsReport.PostingReportEntry(
          date = date,
          businessDate = date,
          type = transaction.transactionType,
          description = transactionTypes[transaction.transactionType]?.description ?: "Unknown",
          transactionUsage = getTransactionUsage(transactionTypes[transaction.transactionType]),
          private = BigDecimal.ZERO,
          spending = BigDecimal.ZERO,
          saving = BigDecimal.ZERO,
          credits = BigDecimal.ZERO,
          debits = BigDecimal.ZERO,
          total = BigDecimal.ZERO,
        )

        // Distribute the transaction amount across the sub-accounts (private, spending, etc.).
        if (account.prisonNumber != null) {
          when (account.accountCode) {
            2101 -> tempPosting.private += entry.amount
            2102 -> tempPosting.spending += entry.amount
            2103 -> tempPosting.saving += entry.amount
          }
        }
        tempPosting
      }
      // 6. Group entries by transaction type to sum up all movements.
      .groupBy { it.type }
      .map { (type, entries) ->
        val firstEntry = entries.first()
        val totalPrivate = entries.sumOf { it.private }
        val totalSpending = entries.sumOf { it.spending }
        val totalSaving = entries.sumOf { it.saving }
        val totalMovement = totalPrivate + totalSpending + totalSaving

        // Assign the total movement to the appropriate credits or debits column.
        val credits = if (firstEntry.transactionUsage == "Receipts") totalMovement else BigDecimal.ZERO
        val debits = if (firstEntry.transactionUsage == "Payments") totalMovement else BigDecimal.ZERO

        // Create the final, aggregated report entry.
        SummaryOfPaymentAndReceiptsReport.PostingReportEntry(
          date = firstEntry.date,
          businessDate = firstEntry.businessDate,
          transactionUsage = firstEntry.transactionUsage,
          type = type,
          description = firstEntry.description,
          private = totalPrivate,
          spending = totalSpending,
          saving = totalSaving,
          credits = credits,
          debits = debits,
          total = totalMovement,
        )
      }
      // 7. Sort the final report
      .sortedWith(compareBy<SummaryOfPaymentAndReceiptsReport.PostingReportEntry> { it.transactionUsage }.thenBy { it.description })
  }

  private fun getTransactionUsage(transactionType: TransactionType?): String = when (transactionType?.txnUsage) {
    "R", "ADV", "C" -> "Receipts"
    "D", "O" -> "Payments"
    "F" -> "Fees"
    else -> "Unknown"
  }
}
