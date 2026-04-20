package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Handlebars
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestTemplateValidationException

class TemplateValidator {

  companion object {
    private val handlebars = Handlebars()
      .registerHelpers(
        TemplateHelpers(
          templateDataFetcherFacade = NoOpTemplateDataFetcherFacade(),
        ),
      )
  }

  fun validateSyntax(template: String): Unit {
    try {
      handlebars.compileInline(template)
    } catch (e: Exception) {
      throw SubjectAccessRequestTemplateValidationException("SAR template failed validation check: ${e.message}", e)
    }
  }

  /**
   * The TemplateDataFetcherFacade isn't used in this context but is required to construct TemplateHelpers -
   * create a NoOp implementation allowing us to create the object without having to satisfy the dependency.
   */
  private class NoOpTemplateDataFetcherFacade : TemplateDataFetcherFacade {
    override fun findPrisonNameByPrisonId(prisonId: String): String? {
      throw UnsupportedOperationException()
    }

    override fun findUserLastNameByUsername(userId: String): String? {
      throw UnsupportedOperationException()
    }

    override fun findLocationNameByNomisId(nomisId: Int): String? {
      throw UnsupportedOperationException()
    }

    override fun findLocationNameByDpsId(dpsId: String): String? {
      throw UnsupportedOperationException()
    }
  }
}