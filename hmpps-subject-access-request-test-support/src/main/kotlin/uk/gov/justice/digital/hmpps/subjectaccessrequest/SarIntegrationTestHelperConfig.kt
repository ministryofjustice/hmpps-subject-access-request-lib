package uk.gov.justice.digital.hmpps.subjectaccessrequest

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@TestConfiguration
open class SarIntegrationTestHelperConfig {

  @Bean
  open fun sarIntegrationTestHelper(
    jwtAuthHelper: JwtAuthorisationHelper,
    @Value("\${hmpps.sar.template.path:}") sarTemplatePath: String,
    @Value("\${hmpps.sar.tests.expected-api-response.path:}") expectedApiResponsePath: String,
    @Value("\${hmpps.sar.tests.expected-render-result.path:}") expectedRenderResultPath: String,
    @Value("\${hmpps.sar.tests.attachments-expected:false}") attachmentsExpected: Boolean,
    @Value("\${hmpps.sar.tests.expected-flyway-schema-version:0}") expectedFlywaySchemaVersion: String,
    @Value("\${hmpps.sar.tests.expected-jpa-entity-schema.path:}") expectedJpaEntitySchemaPath: String,
  ): SarIntegrationTestHelper = SarIntegrationTestHelper(
    jwtAuthHelper,
    sarTemplatePath,
    expectedApiResponsePath,
    expectedRenderResultPath,
    attachmentsExpected,
    expectedFlywaySchemaVersion,
    expectedJpaEntitySchemaPath,
  )
}
