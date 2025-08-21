package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NomisSyncPayloadRepository : JpaRepository<NomisSyncPayload, Long> {

  fun findByRequestId(requestId: UUID): NomisSyncPayload?
  fun findFirstByLegacyTransactionIdOrderByTimestampDesc(transactionId: Long): NomisSyncPayload?

  @Query(
    """
        SELECT p
        FROM NomisSyncPayload p
        WHERE p.transactionTimestamp BETWEEN :startDate AND :endDate
        AND p.requestTypeIdentifier = :requestTypeIdentifier
        AND p.timestamp = (
            SELECT MAX(p2.timestamp)
            FROM NomisSyncPayload p2
            WHERE p2.synchronizedTransactionId = p.synchronizedTransactionId
        )
    """,
  )
  fun findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("requestTypeIdentifier") requestTypeIdentifier: String,
  ): List<NomisSyncPayload>

  fun findFirstBySynchronizedTransactionIdOrderByTimestampDesc(synchronizedTransactionId: UUID): NomisSyncPayload?
}
