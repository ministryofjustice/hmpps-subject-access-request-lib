package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Handlebars

class TemplateRenderService(
  private val templateHelpers: TemplateHelpers,
) {

  fun renderServiceTemplate(params: RenderParameters): String {
    val handlebars = Handlebars()
    handlebars.registerHelpers(templateHelpers)
    val compiledServiceTemplate = handlebars.compileInline(params.template)
    val renderedServiceTemplate = compiledServiceTemplate.apply(params.data)
    return renderedServiceTemplate.toString()
  }
}

data class RenderParameters(val templateVersion: String, val template: String, val data: Any?)
