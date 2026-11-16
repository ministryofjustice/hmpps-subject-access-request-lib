package uk.gov.justice.digital.hmpps.subjectaccessrequest.alerting

import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.UUID

class InternalServiceSlackAlertSender(
  apiUrl: String,
  token: String,
  channelIds: List<String>,
  private val applicationName: String,
  private val environment: String,
) {
  private val client = WebClient.builder().baseUrl(apiUrl).defaultHeaders { it.setBearerAuth(token) }.build()
  private val recipients = channelIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

  init {
    require(token.isNotBlank()) { "Slack bot auth token must be configured" }
    require(recipients.isNotEmpty()) { "At least one HAAR Slack channel ID must be configured" }
  }

  fun sendServiceCallFailedAlert(destination: String, operation: String, requestId: UUID?, serviceName: String?) {
    val text = listOfNotNull(
      "Subject Access Request: Internal Service Call Failed",
      "Environment: $environment",
      "Source: $applicationName",
      "Destination: $destination",
      "Operation: $operation",
      requestId?.let { "SAR ID: $it" },
      serviceName?.let { "Service: $it" },
      "Please investigate the service logs.",
    ).joinToString("\n")

    recipients.forEach { channel ->
      try {
        val response = client.post().uri("/chat.postMessage")
          .bodyValue(mapOf("channel" to channel, "text" to text, "mrkdwn" to false, "unfurl_links" to false, "unfurl_media" to false))
          .retrieve()
          .bodyToMono(SlackResponse::class.java)
          .block(Duration.ofSeconds(5))
        if (response?.ok != true) {
          log.error("Failed to send internal service call Slack alert to {}: {}", channel, response?.error ?: "empty response")
        }
      } catch (ex: Exception) {
        // Alert delivery must not replace the original service-call failure.
        log.error("Failed to send internal service call Slack alert to {}", channel, ex)
      }
    }
  }

  private data class SlackResponse(val ok: Boolean = false, val error: String? = null)

  private companion object {
    private val log = LoggerFactory.getLogger(InternalServiceSlackAlertSender::class.java)
  }
}
