package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.jpa.models.NomisSyncPayload
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncGeneralLedgerTransactionResponse
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionRequest
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.models.sync.SyncOffenderTransactionResponse

@Service
class ResponseMapperService {

  private val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule.Builder().build())

  fun mapToGeneralLedgerTransactionResponse(payload: NomisSyncPayload): SyncGeneralLedgerTransactionResponse {
    val request = objectMapper.readValue<SyncGeneralLedgerTransactionRequest>(payload.body)
    return SyncGeneralLedgerTransactionResponse(
      transactionId = request.transactionId,
      description = request.description,
      reference = request.reference,
      caseloadId = request.caseloadId,
      transactionType = request.transactionType,
      transactionTimestamp = request.transactionTimestamp,
      createdAt = request.createdAt,
      createdBy = request.createdBy,
      createdByDisplayName = request.createdByDisplayName,
      lastModifiedAt = request.lastModifiedAt,
      lastModifiedBy = request.lastModifiedBy,
      lastModifiedByDisplayName = request.lastModifiedByDisplayName,
      generalLedgerEntries = request.generalLedgerEntries,
    )
  }

  fun mapToOffenderTransactionResponse(payload: NomisSyncPayload): SyncOffenderTransactionResponse {
    val request = objectMapper.readValue<SyncOffenderTransactionRequest>(payload.body)
    return SyncOffenderTransactionResponse(
      transactionId = request.transactionId,
      caseloadId = request.caseloadId,
      transactionTimestamp = request.transactionTimestamp,
      createdAt = request.createdAt,
      createdBy = request.createdBy,
      createdByDisplayName = request.createdByDisplayName,
      lastModifiedAt = request.lastModifiedAt,
      lastModifiedBy = request.lastModifiedBy,
      lastModifiedByDisplayName = request.lastModifiedByDisplayName,
      transactions = request.offenderTransactions,
    )
  }
}
