package uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services
import com.google.gson.Gson
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerfinancepocapi.services.events.MergedPrisonerEvent

data class HmppsDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
)

data class AdditionalInformation(
  val nomsNumber: String,
  val removedNomsNumber: String? = null,
  val reason: String? = null,
)

data class Event(val Message: String)

@Service
class DomainEventSubscriber(
  private val gson: Gson,
  private val mergedPrisonerEvent: MergedPrisonerEvent,
) : EventSubscriber {

  @SqsListener("domainevents", factory = "hmppsQueueContainerFactoryProxy")
  fun handleEvents(requestJson: String?) {
    val event = gson.fromJson(requestJson, Event::class.java)
    with(gson.fromJson(event.Message, HmppsDomainEvent::class.java)) {
      when (eventType) {
        "prison-offender-events.prisoner.merged" ->
          mergedPrisonerEvent.handle(
            additionalInformation.removedNomsNumber!!,
            additionalInformation.nomsNumber,
          )
        else -> log.warn("Unexpected domain event received: {}", eventType)
      }
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
