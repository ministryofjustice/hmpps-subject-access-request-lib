package uk.gov.justice.digital.hmpps.subjectaccessrequest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.helger.css.reader.CSSReader
import com.helger.css.writer.CSSWriter
import com.helger.css.writer.CSSWriterSettings
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.RenderParameters
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateDataFetcherFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpers
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateRenderService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import javax.sql.DataSource

open class SarIntegrationTestHelper(
  val jwtAuthHelper: JwtAuthorisationHelper,
  val expectedApiResponsePath: String,
  val expectedRenderResultPath: String,
  val attachmentsExpected: Boolean,
  val expectedFlywaySchemaVersion: String,
  val expectedJpaEntitySchemaPath: String,
  val objectMapper: ObjectMapper,
  val templateDataFetcherFacade: TemplateDataFetcherFacade = mock(),
  val templateHelpers: TemplateHelpers = TemplateHelpers(templateDataFetcherFacade, jacksonObjectMapper()),
  val templateRenderService: TemplateRenderService = TemplateRenderService(templateHelpers),
  val pdfRenderer: PdfRenderer = OpenHtmlPdfRenderer(),
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun <T> requestSarDataForPrn(
    prn: String,
    webTestClient: WebTestClient,
    contentType: Class<T>,
  ): SubjectAccessRequestResponse<T> = requestSarData(prn, null, null, null, webTestClient, contentType)

  fun <T> requestSarDataForPrn(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    webTestClient: WebTestClient,
    contentType: Class<T>,
  ): SubjectAccessRequestResponse<T> = requestSarData(prn, null, fromDate, toDate, webTestClient, contentType)

  fun <T> requestSarDataForCrn(
    crn: String,
    webTestClient: WebTestClient,
    contentType: Class<T>,
  ): SubjectAccessRequestResponse<T> = requestSarData(null, crn, null, null, webTestClient, contentType)

  fun <T> requestSarDataForCrn(
    crn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    webTestClient: WebTestClient,
    contentType: Class<T>,
  ): SubjectAccessRequestResponse<T> = requestSarData(null, crn, fromDate, toDate, webTestClient, contentType)

  fun <T> requestSarData(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    webTestClient: WebTestClient,
    contentType: Class<T>,
  ): SubjectAccessRequestResponse<T> {
    val response = webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParamIfPresent("prn", Optional.ofNullable(prn))
        .queryParamIfPresent("crn", Optional.ofNullable(crn))
        .queryParamIfPresent("fromDate", Optional.ofNullable(fromDate))
        .queryParamIfPresent("toDate", Optional.ofNullable(toDate))
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult().responseBody!!

    return objectMapper.readValue(
      response,
      objectMapper.typeFactory.constructParametricType(SubjectAccessRequestResponse::class.java, contentType),
    )
  }

  fun setAuthorisation(
    username: String? = "TEST_USR",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  fun requestSarTemplate(webTestClient: WebTestClient): String = webTestClient
    .get().uri {
      it.path("/subject-access-request/template")
        .build()
    }
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isOk
    .expectBody(String::class.java)
    .returnResult().responseBody!!

  fun getExpectedSarJson(): String {
    require(expectedApiResponsePath.isNotBlank()) { "SAR expected API response path must be specified via property hmpps.sar.tests.expected-api-response.path" }
    return getResourceAsString(expectedApiResponsePath)
  }

  fun getExpectedRenderResult(): String {
    require(expectedRenderResultPath.isNotBlank()) { "SAR expected render result path must be specified via property hmpps.sar.tests.expected-render-result.path" }
    return getResourceAsString(expectedRenderResultPath)
  }

  fun getExpectedSchemaSnapshot(): String {
    require(expectedJpaEntitySchemaPath.isNotBlank()) { "SAR expected JPA entity schema path must be specified via property hmpps.sar.tests.expected-jpa-entity-schema.path" }
    return getResourceAsString(expectedJpaEntitySchemaPath)
  }

  fun getFlywaySchemaVersion(dataSource: DataSource): String? = Flyway.configure()
    .dataSource(dataSource).load().info().current()?.version?.version

  fun getResourceAsString(path: String): String = this::class.java.getResource(path)?.readText()!!

  fun getResourceAsBytes(path: String): ByteArray = this::class.java.getResource(path)?.readBytes()!!

  fun saveContentToFile(content: String, name: String, path: String = "src/test/resources") {
    fileToGenerate(name, path).writeText(content)
  }

  fun saveContentToFile(content: ByteArray, name: String, path: String = "src/test/resources") {
    fileToGenerate(name, path).writeBytes(content)
  }

  private fun fileToGenerate(name: String, path: String): File {
    val generatedDir = Paths.get(System.getProperty("user.dir"), path)
    Files.createDirectories(generatedDir)
    val file = File(generatedDir.toFile(), name)
    if (!file.exists()) {
      file.createNewFile()
    }
    return file
  }

  fun renderAndSaveReportAsPdf(html: String, prn: String?, crn: String?) {
    val renderedPdf = pdfRenderer.renderSubjectAccessRequestPdf(html, prn, crn)
    saveContentToFile(renderedPdf.toByteArray(), "sar-generated-report.pdf", "build/test-generated")
    log.info("SAVED SAR GENERATED REPORT PDF TO: build/test-generated/sar-generated-report.pdf")
  }

  fun renderServiceReport(
    data: Any?,
    templateVersion: String,
    template: String,
  ): String = templateRenderService.renderServiceTemplate(
    RenderParameters(
      templateVersion = templateVersion,
      template = template,
      data = data,
    ),
    RenderRequestInfo(UUID.randomUUID(), "test-service"),
  ).toString(StandardCharsets.UTF_8)

  fun getGeneratedEntitySchema(entityManager: EntityManager): String {
    val metamodel = entityManager.metamodel
    val currentSchema = metamodel.entities.sortedBy { it.name }.associate { entity ->
      entity.name to entity.attributes.map { it.name to it.javaType.simpleName }.sortedBy { it.first }.toMap()
    }
    return objectMapper.writeValueAsString(currentSchema)
  }

  fun <T> toJson(response: SubjectAccessRequestResponse<T>): String = objectMapper.writeValueAsString(response.content)

  fun assertHtmlEquals(
    actualHtml: String,
    expectedHtml: String,
    description: String? = "Generated report html",
  ) {
    assertThat(normalizeHtml(actualHtml)).`as`(description).isEqualTo(normalizeHtml(expectedHtml))
  }

  fun normalizeHtml(html: String): String {
    val doc = Jsoup.parse(html)
    doc.outputSettings()
      .prettyPrint(true)
      .syntax(OutputSettings.Syntax.html)
      .escapeMode(org.jsoup.nodes.Entities.EscapeMode.base)
      .charset("UTF-8")

    fun sortAttributes(node: Node) {
      if (node is Element) {
        val sortedAttrs = node.attributes().asList().sortedBy { it.key.lowercase() }
        node.clearAttributes()
        sortedAttrs.forEach { node.attr(it.key, it.value) }
      }
      node.childNodes().forEach { sortAttributes(it) }
    }
    sortAttributes(doc)

    doc.select("style").forEach { styleEl ->
      val normalizedCss = normalizeCss(styleEl.data())
      styleEl.text(normalizedCss)
    }

    return doc.outerHtml()
  }

  private fun normalizeCss(css: String): String {
    val stylesheet = CSSReader.readFromString(css) ?: return css

    stylesheet.allStyleRules.forEach { rule ->
      val declarations = rule.allDeclarations.sortedBy { it.property }
      rule.removeAllDeclarations()
      declarations.forEach(rule::addDeclaration)
    }

    val settings = CSSWriterSettings().apply {
      isOptimizedOutput = true
      isRemoveUnnecessaryCode = true
    }

    return CSSWriter(settings).getCSSAsString(stylesheet)
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

  fun stubGetAttachment(url: String, attachmentData: ByteArray) {
    whenever(templateDataFetcherFacade.getRenderableAttachment(argThat { this.url == url }, any())).thenReturn(
      attachmentData,
    )
  }

  fun <T> saveSarApiResponse(response: SubjectAccessRequestResponse<T>) {
    saveContentToFile(toJson(response), "sar-api-response.json.log")
    log.info("SAVED SAR API RESPONSE TO: src/test/resources/sar-api-response.json.log")
  }

  fun saveGeneratedReport(report: String) {
    saveContentToFile(report, "sar-generated-report.html.log")
    log.info("SAVED SAR GENERATED REPORT TO: src/test/resources/sar-generated-report.html.log")
  }

  fun saveEntitySchema(schema: String) {
    saveContentToFile(schema, "entity-schema.json.log")
    log.info("SAVED ENTITY SCHEMA TO: src/test/resources/entity-schema.json.log")
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
data class SubjectAccessRequestResponse<T>(
  val content: T? = null,
  val attachments: List<Any>? = null,
)
