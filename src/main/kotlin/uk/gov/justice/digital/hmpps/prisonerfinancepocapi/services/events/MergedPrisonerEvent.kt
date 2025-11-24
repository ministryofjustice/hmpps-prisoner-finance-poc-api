package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.events

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MergedPrisonerEvent {

  @Transactional
  fun handle(removedPrisonerNumber: String, prisonerNumber: String): String = ""
}
