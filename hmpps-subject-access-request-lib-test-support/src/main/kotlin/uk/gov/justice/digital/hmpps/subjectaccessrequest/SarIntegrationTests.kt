package uk.gov.justice.digital.hmpps.subjectaccessrequest

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import javax.sql.DataSource

interface SarTestBase {

  fun getSarHelper(): SarIntegrationTestHelper
}

interface SarApiTestBase : SarTestBase {

  fun setupTestData()
  fun getPrn(): String? = null
  fun getCrn(): String? = null
  fun getWebTestClientInstance(): WebTestClient
}

interface SarApiDataTest : SarApiTestBase {

  @Test
  fun `SAR API should return expected data`() {
    setupTestData()

    val response = getSarHelper().requestSarData(getPrn(), getCrn(), getWebTestClientInstance())

    assertThat(getSarHelper().toJsonNode(response)).`as`("Response content json")
      .isEqualTo(getSarHelper().getExpectedJsonNode())
    assertThat(response.attachments?.isEmpty() != false).`as`("Response has attachments")
      .isEqualTo(getSarHelper().attachmentsExpected)
  }
}

interface SarTemplateTest : SarApiTestBase {
  @Test
  fun `SAR template should render as expected`() {
    setupTestData()
    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")
    getSarHelper().stubFindLocationNameByNomisIdWith("PROPERTY BOX 1")
    getSarHelper().stubFindLocationNameByDpsIdWith("PROPERTY BOX 2")
    val response = getSarHelper().requestSarData(getPrn(), getCrn(), getWebTestClientInstance())

    val renderResult = getSarHelper().renderServiceTemplate(response.content)

    assertThat(getSarHelper().sanitizeHtml(renderResult)).`as`("Generated report html").isEqualTo(getSarHelper().sanitizeHtml(getSarHelper().getExpectedRenderResult()))
  }
}

interface SarFlywaySchemaTest : SarTestBase {

  fun getDataSourceInstance(): DataSource

  @Test
  fun `Flyway schema version should match expected version`() {
    val flyway = Flyway.configure()
      .dataSource(getDataSourceInstance())
      .load()

    val current = flyway.info().current()
    val expectedVersion = getSarHelper().expectedFlywaySchemaVersion

    assertThat(current?.version?.version).`as`("Flyway schema version").isEqualTo(expectedVersion)
  }
}

interface SarJpaEntitiesTest : SarTestBase {

  fun getEntityManagerInstance(): EntityManager

  @Test
  fun `JPA generated entity schema should match expected snapshot`() {
    val metamodel = getEntityManagerInstance().metamodel
    val currentSchema = metamodel.entities.associate { entity ->
      entity.name to entity.attributes.map { it.name }.sorted()
    }

    val expectedSchema = getSarHelper().getExpectedSchemaSnapshot()

    assertThat(currentSchema).`as`("JPA entity schema").isEqualTo(expectedSchema)
  }
}