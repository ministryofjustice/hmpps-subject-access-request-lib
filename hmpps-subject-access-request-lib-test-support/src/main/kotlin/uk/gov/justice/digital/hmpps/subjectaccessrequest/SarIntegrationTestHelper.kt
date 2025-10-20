package uk.gov.justice.digital.hmpps.subjectaccessrequest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.RenderParameters
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateDataFetcherFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpers
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateRenderService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.util.*

class SarIntegrationTestHelper(
  val jwtAuthHelper: JwtAuthorisationHelper,
  val sarTemplatePath: String,
  val expectedApiResponsePath: String,
  val expectedRenderResultPath: String,
  val attachmentsExpected: Boolean,
  val expectedFlywaySchemaVersion: String,
  val expectedJpaEntitySchemaPath: String,
  val objectMapper: ObjectMapper = ObjectMapper(),
  val templateDataFetcherFacade: TemplateDataFetcherFacade = mock(),
  val templateHelpers: TemplateHelpers = TemplateHelpers(templateDataFetcherFacade),
  val templateRenderService: TemplateRenderService = TemplateRenderService(templateHelpers),
) {

  fun requestSarDataForPrn(prn: String, webTestClient: WebTestClient): SubjectAccessRequestResponse = requestSarData(prn, null, webTestClient)

  fun requestSarDataForCrn(crn: String, webTestClient: WebTestClient): SubjectAccessRequestResponse = requestSarData(null, crn, webTestClient)

  fun requestSarData(prn: String?, crn: String?, webTestClient: WebTestClient): SubjectAccessRequestResponse {
    val response = webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParamIfPresent("prn", Optional.ofNullable(prn))
        .queryParamIfPresent("crn", Optional.ofNullable(crn))
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult().responseBody!!
    return objectMapper.readValue(response, SubjectAccessRequestResponse::class.java)
  }

  internal fun setAuthorisation(
    username: String? = "TEST_USR",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  fun requestSarTemplate(webTestClient: WebTestClient): String {
    return webTestClient.get().uri {
      it.path("/subject-access-request/template")
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult().responseBody!!
  }

  fun getServiceTemplate(): String {
    require(sarTemplatePath.isNotBlank()) { "SAR template path must be specified via property hmpps.sar.template.path" }
    return getResourceAsString(sarTemplatePath)
  }

  fun getExpectedJson(): String {
    require(expectedApiResponsePath.isNotBlank()) { "SAR expected API response path must be specified via property hmpps.sar.tests.expected-api-response.path" }
    return getResourceAsString(expectedApiResponsePath)
  }

  fun getExpectedRenderResult(): String {
    require(expectedRenderResultPath.isNotBlank()) { "SAR expected render result path must be specified via property hmpps.sar.tests.expected-render-result.path" }
    return getResourceAsString(expectedRenderResultPath)
  }

  fun getExpectedSchemaSnapshot(): Map<String, List<String>> {
    require(expectedJpaEntitySchemaPath.isNotBlank()) { "SAR expected JPA entity schema path must be specified via property hmpps.sar.tests.expected-jpa-entity-schema.path" }
    val json = getResourceAsString(expectedJpaEntitySchemaPath)
    val typeRef = object : TypeReference<Map<String, List<String>>>() {}
    return objectMapper.readValue(json, typeRef)
  }

  fun getResourceAsString(path: String): String = this::class.java.getResource(path)?.readText()!!

  fun renderServiceTemplate(data: Any?): String {
    return templateRenderService.renderServiceTemplate(RenderParameters(getServiceTemplate(), data))
  }

  fun getExpectedJsonNode(): JsonNode? = objectMapper.readTree(getExpectedJson())

  fun toJsonNode(response: SubjectAccessRequestResponse): JsonNode? =
    objectMapper.readTree(objectMapper.writeValueAsString(response.content))

  fun sanitizeHtml(html: String): String {
    return html
      .lines()
      .filter { it.isNotBlank() }
      .joinToString("\n")
      .trim()
  }

  fun stubFindPrisonNameWith(prisonName: String) {
    whenever(templateDataFetcherFacade.findPrisonNameByPrisonId(any())).thenReturn(prisonName)
  }

  fun stubFindUserLastNameWith(userLastName: String) {
    whenever(templateDataFetcherFacade.findUserLastNameByUsername(any())).thenReturn(userLastName)
  }

  fun stubFindLocationNameByNomisIdWith(locationName: String) {
    whenever(templateDataFetcherFacade.findLocationNameByNomisId(any())).thenReturn(locationName)
  }

  fun stubFindLocationNameByDpsIdWith(locationName: String) {
    whenever(templateDataFetcherFacade.findLocationNameByDpsId(any())).thenReturn(locationName)
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
data class SubjectAccessRequestResponse(
  val content: Any? = null,
  val attachments: List<Any>? = null,
)