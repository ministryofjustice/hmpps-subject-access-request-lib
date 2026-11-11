package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.StringReader

const val STYLE_TEMPLATE_PATH = "/templates/main_stylesheet.mustache"

abstract class BaseTemplateRenderService(protected val templateHelpers: TemplateHelpers) {

  protected fun getCompiledStyleTemplate(
    params: RenderParameters,
    renderRequestInfo: RenderRequestInfo,
  ): String {
    val handlebars = Handlebars()
    handlebars.registerHelpers(templateHelpers)
    val compiledServiceTemplate = handlebars.compileInline(params.template)

    val context = Context
      .newBuilder(params.data)
      .combine("render-request-info", renderRequestInfo)
      .build()

    return compiledServiceTemplate.apply(context)
  }

  protected fun getCompiledStyleTemplate(): Mustache {
    val defaultMustacheFactory = DefaultMustacheFactory()
    val styleTemplate = getStyleTemplate()
    return defaultMustacheFactory.compile(StringReader(styleTemplate), "styleTemplate")
  }

  protected fun getStyleTemplate() = this::class.java.getResource(STYLE_TEMPLATE_PATH)?.readText() ?: ""
}

data class RenderParameters(val templateVersion: String, val template: String, val data: Any?)
