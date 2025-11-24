package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.domainEvents
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration.IntegrationTestBase
class DomainEventQueueIntegrationTest : IntegrationTestBase() {

  @Test
  fun `will merge restricted patient`() {
//    dischargePrisonerWebClient(prisonerNumber = "A12345")
//      .exchange()
//      .expectStatus().isCreated
//
//    getRestrictedPatient(prisonerNumber = "A12345")
//      .exchange()
//      .expectStatus().is2xxSuccessful

//    domainEventQueue.run {
//      sqsClient.sendMessage(
//        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(makePrisonerMergeEvent("A12345", "A23456")).build(),
//      ).get()
//
//      await untilCallTo { sqsClient.countMessagesOnQueue(queueUrl).get() } matches { it == 0 }
//    }

    // TODO: Add in implementation of merge event
  }
}
