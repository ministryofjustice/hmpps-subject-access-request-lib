package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.mustachejava.DefaultMustacheFactory
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.nio.charset.StandardCharsets

const val STYLE_TEMPLATE_PATH = "/templates/main_stylesheet.mustache"

class TemplateRenderService(
  private val templateHelpers: TemplateHelpers,
) {

  fun renderServiceTemplate(params: RenderParameters, renderRequestInfo: RenderRequestInfo): ByteArrayOutputStream {
    val handlebars = Handlebars()
    handlebars.registerHelpers(templateHelpers)
    val compiledServiceTemplate = handlebars.compileInline(params.template)

    val context = Context
      .newBuilder(params.data)
      .combine("render-request-info", renderRequestInfo)
      .build()

    val renderedServiceReport = compiledServiceTemplate.apply(context)
    return renderStyleTemplate(renderedServiceReport)
  }

  private fun renderStyleTemplate(renderedServiceTemplate: String): ByteArrayOutputStream {
    val defaultMustacheFactory = DefaultMustacheFactory()
    val styleTemplate = getStyleTemplate()
    val compiledStyleTemplate = defaultMustacheFactory.compile(StringReader(styleTemplate), "styleTemplate")

    val out = ByteArrayOutputStream()
    BufferedWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer ->
      compiledStyleTemplate.execute(
        writer,
        mapOf("serviceTemplate" to renderedServiceTemplate),
      ).flush()
    }
    return out
  }

  private fun getStyleTemplate() = this::class.java.getResource(STYLE_TEMPLATE_PATH)?.readText() ?: ""
}

data class RenderParameters(val templateVersion: String, val template: String, val data: Any?)
