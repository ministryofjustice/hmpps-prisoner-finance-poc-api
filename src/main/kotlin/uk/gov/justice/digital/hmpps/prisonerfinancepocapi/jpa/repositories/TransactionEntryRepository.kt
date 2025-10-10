package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
import java.math.BigDecimal
import java.sql.Timestamp

@Repository
interface TransactionEntryRepository : JpaRepository<TransactionEntry, Long> {
  fun findByTransactionId(transactionId: Long): List<TransactionEntry>
  fun findByAccountId(accountId: Long): List<TransactionEntry>

  @Query(
    """
    SELECT te FROM TransactionEntry te 
    JOIN Transaction t ON te.transactionId = t.id 
    WHERE t.date BETWEEN :startDate AND :endDate 
    AND t.prison = :prisonCode
""",
  )
  fun findByDateBetweenAndPrisonCode(startDate: Timestamp, endDate: Timestamp, prisonCode: String): List<TransactionEntry>

  @Query(
    """
        SELECT 
            SUM(
                CASE
                    WHEN te.entry_type = 'CR' THEN te.amount
                    WHEN te.entry_type = 'DR' THEN -te.amount
                    ELSE 0 
                END
            )
        FROM 
            transaction_entry te
        JOIN 
            transaction t ON te.transaction_id = t.id
        JOIN 
            account a ON te.account_id = a.id
        WHERE
            a.account_code = :accountCode AND t.prison = :prison
    """,
    nativeQuery = true,
  )
  fun calculateAggregatedBalance(
    accountCode: Int,
    prison: String,
  ): BigDecimal?
}
