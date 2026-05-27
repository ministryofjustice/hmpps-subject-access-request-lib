package uk.gov.justice.digital.hmpps.subjectaccessrequest

import jakarta.persistence.EntityManager
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import javax.sql.DataSource

interface SarTestBase {

  /**
   *  Returns the [SarIntegrationTestHelper] instance to be used in the test implementations.
   *  
   *  @return a [SarIntegrationTestHelper] instance
   */
  fun getSarHelper(): SarIntegrationTestHelper
}

interface SarApiTestBase : SarTestBase {

  /**
   * The implementation of this should do any data setup required for the test.  
   */
  fun setupTestData()

  /**
   * Override this to specify a PRN to be used to fetch the SAR data via the API call.
   * 
   * @return the PRN to use when fetching data
   */
  fun getPrn(): String? = null

  /**
   * Override this to specify a CRN to be used to fetch the SAR data via the API call.
   * If the [getPrn] method is also overridden then this will be ignored
   *
   * @return the CRN to use when fetching data
   */
  fun getCrn(): String? = null

  /**
   * Override this to specify a start date to be used to fetch the SAR data via the API call.
   *
   * @return the start date to use when fetching data
   */
  fun getFromDate(): LocalDate? = null

  /**
   * Override this to specify an end date to be used to fetch the SAR data via the API call.
   * 
   * @return the end date to use when fetching data
   */
  fun getToDate(): LocalDate? = null

  /**
   * Returns the [WebTestClient] instance to be used in the test implementations.
   * 
   * @return a [WebTestClient] instance
   */
  fun getWebTestClientInstance(): WebTestClient

  /**
   * Override this to define explicit type of the content section of the SAR Data API response.
   * 
   * @return the type of the content section, default is [Any]
   */
  fun getContentType(): Class<*> = Any::class.java
}

/**
 * Implement this interface to add a test that verifies there are no changes to the SAR data API by calling it to
 * retrieve the JSON data, and then comparing to an expected JSON response specified via the property 
 * `hmpps.sar.tests.expected-api-response.path`.
 * 
 * The property `hmpps.sar.tests.attachments-expected` should be set to `true` if non-inline attachments exist.
 * 
 * Run with the env var `SAR_GENERATE_ACTUAL` with a value of `true` to generate the initial expected json response
 * under `src/test/resources/sar-api-response.json.log`.
 * 
 * See <a href="https://github.com/ministryofjustice/hmpps-subject-access-request-lib#api-test">README</a> for more
 * details.
 */
interface SarApiDataTest : SarApiTestBase {

  @Test
  fun `SAR API should return expected data`() {
    setupTestData()

    val response = getSarHelper().requestSarData(getPrn(), getCrn(), getFromDate(), getToDate(), getWebTestClientInstance(), getContentType())
    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      getSarHelper().saveSarApiResponse(response)
    } else {
      assertThatJson(getSarHelper().toJson(response)).`as`("Response content json")
        .isEqualTo(getSarHelper().getExpectedSarJson())
      assertThat(response.attachments?.isNotEmpty() == true).`as`("Response has attachments")
        .isEqualTo(getSarHelper().attachmentsExpected)
    }
  }
}

/**
 * Implement this interface to add a test that verifies there are no changes to the SAR report by calling the SAR data
 * API to retrieve the JSON data, using that to then generate a HTML report via the mustache template, and then
 * comparing to an expected version of the report specified via the property
 * `hmpps.sar.tests.expected-render-result.path`.
 * 
 * Run with the env var `SAR_GENERATE_ACTUAL` with a value of `true` to generate the initial expected report under
 * `src/test/resources/sar-generated-report.html.log`
 * 
 * See <a href="https://github.com/ministryofjustice/hmpps-subject-access-request-lib#report-generation-test">README</a>
 * for more details.
 */
interface SarReportTest : SarApiTestBase {

  /**
   * Override this to specify inline attachment files that will be rendered in the report.
   * The implementation should return a map matching the download `url` in the data response
   * to the resource file path.
   *
   * @return a [Map] of the inline attachment url to file path
   */
  fun getInlineAttachments(): Map<String, String> = emptyMap()

  @Test
  fun `SAR report should render as expected`() {
    setupTestData()
    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")
    getSarHelper().stubFindLocationNameByNomisIdWith("PROPERTY BOX 1")
    getSarHelper().stubFindLocationNameByDpsIdWith("PROPERTY BOX 2")
    getInlineAttachments().forEach { attachment ->
      getSarHelper().stubGetAttachment(attachment.key, getSarHelper().getResourceAsBytes(attachment.value))
    }
    val dataResponse = getSarHelper().requestSarData(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
      getWebTestClientInstance(),
      getContentType(),
    )
    val templateResponse = getSarHelper().requestSarTemplate(getWebTestClientInstance())

    val renderResult = getSarHelper().renderServiceReport(
      data = dataResponse.content,
      templateVersion = "1.0",
      template = templateResponse,
    )

    getSarHelper().renderAndSaveReportAsPdf(renderResult, getPrn(), getCrn())

    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      getSarHelper().saveGeneratedReport(renderResult)
    } else {
      getSarHelper().assertHtmlEquals(renderResult, getSarHelper().getExpectedRenderResult())
    }
  }
}

/**
 * Implement this interface to add a test that verifies there are no Flyway changes by comparing the Flyway schema
 * version number to the expected version specified via the property `hmpps.sar.tests.expected-flyway-schema-version`.
 *
 * See <a href="https://github.com/ministryofjustice/hmpps-subject-access-request-lib#data-schema-tests">README</a> for
 * more details. 
 */
interface SarFlywaySchemaTest : SarTestBase {

  /**
   * Returns the [DataSource] instance to be used in the [SarFlywaySchemaTest] test implementation for determining the
   * Flyway schema version.
   *
   * @return a [DataSource] instance
   */
  fun getDataSourceInstance(): DataSource

  @Test
  fun `Flyway schema version should match expected version`() {
    val currentVersion = getSarHelper().getFlywaySchemaVersion(getDataSourceInstance())
    val expectedVersion = getSarHelper().expectedFlywaySchemaVersion

    assertThat(currentVersion).`as`("Flyway schema version").isEqualTo(expectedVersion)
  }
}

/**
 * Implement this interface to add a test that verifies there are no changes to the JPA Entities in a service by
 * generating a schema of JPA Entities and comparing to an expected schema specified via the property
 * `hmpps.sar.tests.expected-jpa-entity-schema.path`.
 *
 * See <a href="https://github.com/ministryofjustice/hmpps-subject-access-request-lib#data-schema-tests">README</a> for
 * more details.
 */
interface SarJpaEntitiesTest : SarTestBase {

  /**
   * Returns the [EntityManager] instance to be used in the [SarJpaEntitiesTest] test implementation for generating the
   * entity schema.
   *
   * @return a [EntityManager] instance
   */
  fun getEntityManagerInstance(): EntityManager

  @Test
  fun `JPA generated entity schema should match expected snapshot`() {
    val currentSchema = getSarHelper().getGeneratedEntitySchema(getEntityManagerInstance())
    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      getSarHelper().saveEntitySchema(currentSchema)
    } else {
      assertThatJson(currentSchema).`as`("JPA entity schema")
        .isEqualTo(getSarHelper().getExpectedSchemaSnapshot())
    }
  }
}
