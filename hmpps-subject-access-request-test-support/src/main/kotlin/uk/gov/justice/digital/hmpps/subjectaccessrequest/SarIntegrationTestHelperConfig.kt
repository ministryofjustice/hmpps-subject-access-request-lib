package uk.gov.justice.digital.hmpps.subjectaccessrequest

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@TestConfiguration
open class SarIntegrationTestHelperConfig {

  @Bean
  open fun sarIntegrationTestHelper(
    jwtAuthHelper: JwtAuthorisationHelper,
    @Value("\${hmpps.sar.tests.expected-api-response.path:}") expectedApiResponsePath: String,
    @Value("\${hmpps.sar.tests.expected-render-result.path:}") expectedRenderResultPath: String,
    @Value("\${hmpps.sar.tests.attachments-expected:false}") attachmentsExpected: Boolean,
    @Value("\${hmpps.sar.tests.expected-flyway-schema-version:0}") expectedFlywaySchemaVersion: String,
    @Value("\${hmpps.sar.tests.expected-jpa-entity-schema.path:}") expectedJpaEntitySchemaPath: String,
    objectMapper: ObjectMapper?,
  ): SarIntegrationTestHelper = SarIntegrationTestHelper(
    jwtAuthHelper,
    expectedApiResponsePath,
    expectedRenderResultPath,
    attachmentsExpected,
    expectedFlywaySchemaVersion,
    expectedJpaEntitySchemaPath,
    objectMapper ?: defaultObjectMapper(),
  )

  private fun defaultObjectMapper(): ObjectMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModule(JavaTimeModule())
    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()
}
