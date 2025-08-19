package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NomisSyncPayloadRepository : JpaRepository<NomisSyncPayload, Long> {

  fun findByRequestTypeIdentifier(requestType: String): List<NomisSyncPayload>
  fun findByRequestId(requestId: UUID): List<NomisSyncPayload>
  fun findByTransactionId(transactionId: Long): List<NomisSyncPayload>
  fun findByCaseloadId(caseloadId: String): List<NomisSyncPayload>
  fun findAllByTransactionTimestampBetweenAndRequestTypeIdentifier(startDate: LocalDateTime, endDate: LocalDateTime, requestTypeIdentifier: String): List<NomisSyncPayload>
  fun findBySynchronizedTransactionId(synchronizedTransactionId: UUID): NomisSyncPayload?
}
