package uk.gov.justice.digital.hmpps.subjectaccessrequest.testapp

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.testapp.service.SubjectAccessRequestContent
import javax.sql.DataSource

@ActiveProfiles("test")
class SubjectAccessRequestIntegrationTest :
  SarIntegrationTestBase(),
  SarApiDataTest,
  SarReportTest,
  SarFlywaySchemaTest,
  SarJpaEntitiesTest {

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  override fun setupTestData() {
    // Data setup
  }

  override fun getPrn(): String? = "A1234AA"

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getInlineAttachments(): Map<String, String> = mapOf(
    "http://image.png" to "/sar/image.png",
    "http://map.jpg" to "/sar/map.jpg",
  )

  override fun getContentType(): Class<*> = SubjectAccessRequestContent::class.java
}
