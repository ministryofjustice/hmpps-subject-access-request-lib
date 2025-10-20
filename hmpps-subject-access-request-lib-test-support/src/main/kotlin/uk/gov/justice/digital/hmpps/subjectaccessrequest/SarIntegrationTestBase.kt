package uk.gov.justice.digital.hmpps.subjectaccessrequest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(SarIntegrationTestHelperConfig::class)
abstract class SarIntegrationTestBase : SarApiTestBase {

  @Autowired
  protected lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  override fun getWebTestClientInstance() = webTestClient
  override fun getSarHelper() = sarIntegrationTestHelper
}
