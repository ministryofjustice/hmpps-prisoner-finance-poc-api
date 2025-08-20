package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.repositories.NomisSyncPayloadRepository
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionResponse
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SyncQueryService(
  private val nomisSyncPayloadRepository: NomisSyncPayloadRepository,
  private val responseMapperService: ResponseMapperService,
) {

  fun findByRequestId(requestId: UUID): NomisSyncPayload? = nomisSyncPayloadRepository.findByRequestId(requestId).firstOrNull()

  fun findByTransactionId(transactionId: Long): NomisSyncPayload? = nomisSyncPayloadRepository.findByTransactionId(transactionId).firstOrNull()

  fun findNomisSyncPayloadBySynchronizedTransactionId(synchronizedTransactionId: UUID): NomisSyncPayload? = nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(synchronizedTransactionId)

  fun findGeneralLedgerTransactionsByTimestampBetween(startDate: LocalDate, endDate: LocalDate): List<NomisSyncPayload> {
    val userZone = ZoneId.of("Europe/London")

    // Convert the start and end of the user's local day to UTC
    val startOfUserDayInUtc = ZonedDateTime.of(startDate.atStartOfDay(), userZone).withZoneSameInstant(ZoneOffset.UTC)
    val endOfUserDayInUtc = ZonedDateTime.of(endDate.plusDays(1).atStartOfDay(), userZone).withZoneSameInstant(ZoneOffset.UTC)

    return nomisSyncPayloadRepository.findAllByTransactionTimestampBetweenAndRequestTypeIdentifier(
      startOfUserDayInUtc.toLocalDateTime(),
      endOfUserDayInUtc.toLocalDateTime(),
      SyncGeneralLedgerTransactionRequest::class.simpleName!!,
    )
  }

  fun getGeneralLedgerTransactionsByDate(startDate: LocalDate, endDate: LocalDate): List<SyncGeneralLedgerTransactionResponse> {
    val nomisPayloads = findGeneralLedgerTransactionsByTimestampBetween(startDate, endDate)
    return nomisPayloads.map { payload ->
      responseMapperService.mapToGeneralLedgerTransactionResponse(payload)
    }
  }

  fun getGeneralLedgerTransactionById(id: UUID): SyncGeneralLedgerTransactionResponse? {
    val payload = findNomisSyncPayloadBySynchronizedTransactionId(id)
    return if (payload != null && payload.requestTypeIdentifier == SyncGeneralLedgerTransactionRequest::class.simpleName) {
      responseMapperService.mapToGeneralLedgerTransactionResponse(payload)
    } else {
      null
    }
  }

  fun getOffenderTransactionById(id: UUID): SyncOffenderTransactionResponse? {
    val payload = findNomisSyncPayloadBySynchronizedTransactionId(id)
    return if (payload != null && payload.requestTypeIdentifier == SyncOffenderTransactionRequest::class.simpleName) {
      responseMapperService.mapToOffenderTransactionResponse(payload)
    } else {
      null
    }
  }
}
