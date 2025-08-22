package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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
import kotlin.reflect.KClass

@Service
class SyncQueryService(
  private val nomisSyncPayloadRepository: NomisSyncPayloadRepository,
  private val responseMapperService: ResponseMapperService,
) {

  fun findByRequestId(requestId: UUID): NomisSyncPayload? = nomisSyncPayloadRepository.findByRequestId(requestId)

  fun findByLegacyTransactionId(transactionId: Long): NomisSyncPayload? = nomisSyncPayloadRepository.findFirstByLegacyTransactionIdOrderByTimestampDesc(transactionId)

  fun findNomisSyncPayloadBySynchronizedTransactionId(synchronizedTransactionId: UUID): NomisSyncPayload? = nomisSyncPayloadRepository.findFirstBySynchronizedTransactionIdOrderByTimestampDesc(synchronizedTransactionId)

  fun getGeneralLedgerTransactionsByDate(
    startDate: LocalDate,
    endDate: LocalDate,
    page: Int,
    size: Int,
  ): Page<SyncGeneralLedgerTransactionResponse> {
    val pageable = PageRequest.of(page, size)
    val nomisPayloadsPage = findNomisSyncPayloadsByTimestampAndType(startDate, endDate, SyncGeneralLedgerTransactionRequest::class, pageable)
    return nomisPayloadsPage.map { payload ->
      responseMapperService.mapToGeneralLedgerTransactionResponse(payload)
    }
  }

  fun getOffenderTransactionsByDate(
    startDate: LocalDate,
    endDate: LocalDate,
    page: Int,
    size: Int,
  ): Page<SyncOffenderTransactionResponse> {
    val pageable = PageRequest.of(page, size)
    val nomisPayloadsPage = findNomisSyncPayloadsByTimestampAndType(startDate, endDate, SyncOffenderTransactionRequest::class, pageable)
    return nomisPayloadsPage.map { payload ->
      responseMapperService.mapToOffenderTransactionResponse(payload)
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

  private fun findNomisSyncPayloadsByTimestampAndType(
    startDate: LocalDate,
    endDate: LocalDate,
    requestType: KClass<*>,
    pageable: Pageable,
  ): Page<NomisSyncPayload> {
    val userZone = ZoneId.of("Europe/London")

    val startOfUserDayInUtc = ZonedDateTime.of(startDate.atStartOfDay(), userZone).withZoneSameInstant(ZoneOffset.UTC)
    val endOfUserDayInUtc = ZonedDateTime.of(endDate.plusDays(1).atStartOfDay(), userZone).withZoneSameInstant(ZoneOffset.UTC)

    return nomisSyncPayloadRepository.findLatestByTransactionTimestampBetweenAndRequestTypeIdentifier(
      startOfUserDayInUtc.toLocalDateTime(),
      endOfUserDayInUtc.toLocalDateTime(),
      requestType.simpleName!!,
      pageable,
    )
  }
}
