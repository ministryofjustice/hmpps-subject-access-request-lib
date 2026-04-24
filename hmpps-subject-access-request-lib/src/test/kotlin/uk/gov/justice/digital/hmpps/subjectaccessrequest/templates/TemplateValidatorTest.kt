package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestTemplateValidationException

class TemplateValidatorTest {

  private val validator = TemplateValidator()

  @Test
  fun `should return successfully validate template with valid syntax`() {
    validator.validateSyntax(getTemplate("test-service-template"))
  }

  @Test
  fun `should return successfully validate real world template`() {
    validator.validateSyntax(getTemplate("real-world-template"))
  }

  @Test
  fun `should return successfully validate inline attachment template`() {
    validator.validateSyntax(getTemplate("inline-attachment-template"))
  }

  @Test
  fun `should throw exception for template with missing closing tag`() {
    val actual = assertThrows<SubjectAccessRequestTemplateValidationException> {
      validator.validateSyntax(getTemplate("missing-tag"))
    }

    assertThat(actual.message).startsWith("SAR template failed validation check: ")
    assertThat(actual.message).contains("expected: '{{/'")
  }

  @Test
  fun `should throw exception for template with unknown custom helper`() {
    val actual = assertThrows<SubjectAccessRequestTemplateValidationException> {
      validator.validateSyntax(getTemplate("unknown-helper"))
    }

    assertThat(actual.message).startsWith("SAR template failed validation check: ")
    assertThat(actual.message).contains("could not find helper: 'fibonacci'")
  }

  @Test
  fun `should throw exception for template with invalid opening tag`() {
    val actual = assertThrows<SubjectAccessRequestTemplateValidationException> {
      validator.validateSyntax(getTemplate("invalid-open-tag"))
    }

    assertThat(actual.message).startsWith("SAR template failed validation check: ")
    assertThat(actual.message).contains("found: '}'")
  }

  private fun getTemplate(
    filename: String,
  ): String = this.javaClass.getResourceAsStream("/templates/$filename.mustache")
    .use { String(it.readAllBytes()) }
}
