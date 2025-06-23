package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "nomis_sync_payloads")
data class NomisSyncPayload(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  val timestamp: LocalDateTime,

  @Column(name = "transaction_id")
  val transactionId: Int?,

  @Column(name = "request_id")
  val requestId: UUID?,

  @Column(name = "caseload_id")
  val caseloadId: String?,

  @Column(name = "request_type_identifier")
  val requestTypeIdentifier: String?,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  val body: String,
)
