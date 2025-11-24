package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events

data class PrisonerMergedEvent(
  val personReference: PersonReference,
  val occurredAt: String,
  val description: String,
  val eventType: String,
  val version: Int,
  var rawMessage: String,
  val publishedAt: String,
  val additionalInformation: AdditionalInformation,
)

data class PersonReference(
  val identifiers: List<Identifier>,
)

data class Identifier(
  val type: String,
  val value: String,
)

data class AdditionalInformation(
  var nomsNumber: String,
  val reason: String,
  val bookingId: String,
  var removedNomsNumber: String,
)
