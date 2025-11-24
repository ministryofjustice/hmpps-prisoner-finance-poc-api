package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.config.TAG_PRISON_ACCOUNTS
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events.PrisonerMergedEvent

@Tag(name = TAG_PRISON_ACCOUNTS)
@RestController
class DPSEventsController {

  @PostMapping("/mergePrisonerEvent")
  fun mergePrisonerEvent(
    @PathVariable event: PrisonerMergedEvent,
  ): ResponseEntity<String> {
    if (event.additionalInformation.nomsNumber.isNullOrBlank() ||
      event.additionalInformation.removedNomsNumber.isNullOrBlank()
    ) {
      return ResponseEntity.badRequest().body("Additional Information must not be null or empty")
    }
    return ResponseEntity.ok("")
  }
}
