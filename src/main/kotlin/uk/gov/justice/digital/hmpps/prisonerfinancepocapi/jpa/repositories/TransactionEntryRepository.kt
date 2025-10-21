package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.entities.TransactionEntry
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
}
