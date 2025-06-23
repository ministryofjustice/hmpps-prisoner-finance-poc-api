package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.domain.NomisSyncPayload
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NomisSyncPayloadRepository : JpaRepository<NomisSyncPayload, Long> {

  fun findByRequestTypeIdentifier(requestType: String): List<NomisSyncPayload>
  fun findByRequestId(requestId: UUID): List<NomisSyncPayload>
  fun findByTimestampBetweenOrderByTimestampAsc(start: LocalDateTime, end: LocalDateTime): List<NomisSyncPayload>

  fun findByTransactionId(transactionId: Int): List<NomisSyncPayload>
  fun findByCaseloadId(caseloadId: String): List<NomisSyncPayload>

  @Query(
    value = "SELECT ap.* FROM nomis_sync_payloads ap, jsonb_array_elements(ap.body->'offenderTransactions') AS ot WHERE ot->>'offenderId' = :offenderId",
    nativeQuery = true,
  )
  fun findByOffenderIdInNestedOffenderTransactions(@Param("offenderId") offenderId: String): List<NomisSyncPayload>

  @Query(value = "SELECT ap.* FROM nomis_sync_payloads ap WHERE ap.body->>'transactionId' = :transactionIdStr", nativeQuery = true)
  fun findByTransactionIdInBody(@Param("transactionIdStr") transactionIdStr: String): List<NomisSyncPayload>
}
