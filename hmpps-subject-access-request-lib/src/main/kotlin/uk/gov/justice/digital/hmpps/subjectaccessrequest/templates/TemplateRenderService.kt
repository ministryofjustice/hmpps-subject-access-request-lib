package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Handlebars
import com.github.mustachejava.DefaultMustacheFactory
import org.springframework.beans.factory.annotation.Value
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.nio.charset.StandardCharsets

class TemplateRenderService(
  private val templateHelpers: TemplateHelpers,
  @param:Value("\${template-resources.directory}") private val templatesDirectory: String = "/templates",
) {

  fun renderServiceTemplate(params: RenderParameters): ByteArrayOutputStream {
    val handlebars = Handlebars()
    handlebars.registerHelpers(templateHelpers)
    val compiledServiceTemplate = handlebars.compileInline(params.template)
    val renderedServiceReport = compiledServiceTemplate.apply(params.data)
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

  private fun getStyleTemplate(): String = getTemplateResourceOrNull("$templatesDirectory/main_stylesheet.mustache") ?: ""

  private fun getTemplateResourceOrNull(path: String) = this::class.java.getResource(path)?.readText()
}

data class RenderParameters(val templateVersion: String, val template: String, val data: Any?)
