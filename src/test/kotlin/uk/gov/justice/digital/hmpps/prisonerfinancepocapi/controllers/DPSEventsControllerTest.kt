package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks

import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events.AdditionalInformation
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events.Identifier
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events.PersonReference
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.events.PrisonerMergedEvent



@ExtendWith(MockitoExtension::class)
class DPSEventsControllerTest {

  @InjectMocks
  private lateinit var dpsEventsController: DPSEventsController

  private lateinit var mockPrisonerMergedEvent: PrisonerMergedEvent

  @BeforeEach
  fun setup() {
    dpsEventsController = DPSEventsController()

    mockPrisonerMergedEvent = PrisonerMergedEvent(
      personReference = PersonReference(
        identifiers = listOf(
          Identifier(type = "NOMS", value = "A8515EC")
        )
      ),
      occurredAt = "2024-08-15T10:23:45.000Z",
      description = "A prisoner has been merged from A8515EC to A8516EC",
      eventType = "prison-offender-events.prisoner.merged",
      version = 1,
      rawMessage = """
{
    "Type" : "Notification",
    "MessageId" : "3ddc40a1-00b3-5e2d-8afd-21303ba46237",
    "TopicArn" : "arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-e29fb030a51b3576dd645aa5e460e573",
    "Message" : {
      "version":1,
      "eventType":"prison-offender-events.prisoner.merged",
      "description":"A prisoner has been merged from A8515EC to A8516EC",
      "occurredAt":"2025-11-14T14:12:39Z",
      "publishedAt":"2025-11-14T14:12:39.692577186Z",
      "personReference":{
          "identifiers":[{"type":"NOMS","value":"A8515EC"}]},
          "additionalInformation":{
              "bookingId":"1231893",
              "nomsNumber":"A8515EC",
              "removedNomsNumber":"A8516EC",
              "reason":"MERGE"}
      },
      "Timestamp" : "2025-11-14T14:12:39.726Z",
      "SignatureVersion" : "1",  
      "Signature" : "uDKx7fjqyWmF2aTilvr4wmtmumjyerhdFSkQExxSx6QX0dKM2x3ERWq9jYVAWIxWGRoMNVzMzI+wNZjyUzuFGIS6XklQQrUcyKiSXSiE80k6HjMDAJQoah2cKKYNM1BIR9xnYFSvHGz8QP2ru0p6DIdZ+ob1n9ppwCnRI8MWbmQhoDToc4lUWi6qTxS0klaGSJRxgvuzu2OYe5QB2vBwX7UrvKGR0CDRV7PiADodWVHhm9lsSCffUgcDlm7CO/6VeVYAOX1uM+bUFor8gae6n923/DBbAju6hMEPRucelzp6Y/YJ8MPa2e/JY7ySqT6UDAi6NuzeEhqOL0w5pmyToQ==",
      "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-6209c161c6221fdf56ec1eb5c821d112.pem",
      "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-e29fb030a51b3576dd645aa5e460e573:842c6089-3928-4937-9df8-6807e24c0be4",
      "MessageAttributes" : {
          "traceparent" : {"Type":"String","Value":"00-13b3f16331da810226981080088cdf6d-82881edbb9bae825-01"},
          "eventType" : {"Type":"String","Value":"prison-offender-events.prisoner.merged"} 
      }
}    """,
      publishedAt = "2024-08-15T10:24:00.000Z",
      additionalInformation = AdditionalInformation(
        nomsNumber = "A8515EC",
        reason = "MERGE",
        bookingId = "123456",
        removedNomsNumber = "A8516EC"
      )
    )
  }

  @Test
  fun `MergePrisonerEvent should return ok status`() {
    val response = dpsEventsController.mergePrisonerEvent(mockPrisonerMergedEvent)

    assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.OK)
  }

  @Test
  fun `MergePrisonerEvent fails when bad request when nomsNumber or removedNomsNumber is empty`() {
    mockPrisonerMergedEvent.additionalInformation.nomsNumber = ""
    val response = dpsEventsController.mergePrisonerEvent(mockPrisonerMergedEvent)

    assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST)

    mockPrisonerMergedEvent.additionalInformation.removedNomsNumber = ""
    val response2 = dpsEventsController.mergePrisonerEvent(mockPrisonerMergedEvent)

    assertThat(response2.statusCode).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST)
  }
}