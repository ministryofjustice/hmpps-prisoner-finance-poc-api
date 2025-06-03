package uk.gov.justice.digital.hmpps.prisonerfinancepocapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrisonerFinancePocApi

fun main(args: Array<String>) {
  runApplication<PrisonerFinancePocApi>(*args)
}
