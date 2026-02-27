package uk.gov.justice.digital.hmpps.subjectaccessrequest

import jakarta.persistence.EntityManager
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import javax.sql.DataSource

interface SarTestBase {

  fun getSarHelper(): SarIntegrationTestHelper
}

interface SarApiTestBase : SarTestBase {

  fun setupTestData()
  fun getPrn(): String? = null
  fun getCrn(): String? = null
  fun getFromDate(): LocalDate? = null
  fun getToDate(): LocalDate? = null
  fun getWebTestClientInstance(): WebTestClient
}

interface SarApiDataTest : SarApiTestBase {

  @Test
  fun `SAR API should return expected data`() {
    setupTestData()

    val response = getSarHelper().requestSarData(getPrn(), getCrn(), getFromDate(), getToDate(), getWebTestClientInstance())
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

interface SarReportTest : SarApiTestBase {
  @Test
  fun `SAR report should render as expected`() {
    setupTestData()
    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")
    getSarHelper().stubFindLocationNameByNomisIdWith("PROPERTY BOX 1")
    getSarHelper().stubFindLocationNameByDpsIdWith("PROPERTY BOX 2")
    val dataResponse = getSarHelper().requestSarData(getPrn(), getCrn(), getFromDate(), getToDate(), getWebTestClientInstance())
    val templateResponse = getSarHelper().requestSarTemplate(getWebTestClientInstance())

    val renderResult = getSarHelper().renderServiceReport(
      data = dataResponse.content,
      templateVersion = "1.0",
      template = templateResponse,
    )
    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      getSarHelper().saveGeneratedReport(renderResult)
    } else {
      getSarHelper().assertHtmlEquals(renderResult, getSarHelper().getExpectedRenderResult())
    }
  }
}

interface SarFlywaySchemaTest : SarTestBase {

  fun getDataSourceInstance(): DataSource

  @Test
  fun `Flyway schema version should match expected version`() {
    val currentVersion = getSarHelper().getFlywaySchemaVersion(getDataSourceInstance())
    val expectedVersion = getSarHelper().expectedFlywaySchemaVersion

    assertThat(currentVersion).`as`("Flyway schema version").isEqualTo(expectedVersion)
  }
}

interface SarJpaEntitiesTest : SarTestBase {

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
