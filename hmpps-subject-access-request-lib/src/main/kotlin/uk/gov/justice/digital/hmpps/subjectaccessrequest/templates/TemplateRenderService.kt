package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.text.Charsets.UTF_8

const val STYLE_TEMPLATE_PATH = "/templates/main_stylesheet.mustache"

class TemplateRenderService(
  private val templateHelpers: TemplateHelpers,
) {

  @Deprecated(
    message = """
      Replaced with TemplateRenderService#renderServiceTemplateToFile which renders data to a temporary file on disk to 
      prevent the process consuming the app memory when handling large volumes of data. 
      This legacy version is fine for use in testing/verifying template content for sign off but shouldn't be used in 
      the SAR prod environment. 
      """,
  )
  fun renderServiceTemplate(
    params: RenderParameters,
    renderRequestInfo: RenderRequestInfo,
  ): ByteArrayOutputStream {
    val renderedServiceReport = getCompiledStyleTemplate(params, renderRequestInfo)
    val compiledStyleTemplate = getCompiledStyleTemplate()

    val out = ByteArrayOutputStream()
    BufferedWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer ->
      compiledStyleTemplate.execute(
        writer,
        mapOf("serviceTemplate" to renderedServiceReport),
      ).flush()
    }
    return out
  }

  fun renderServiceTemplateToFile(
    params: RenderParameters,
    renderRequestInfo: RenderRequestInfo,
    outputTarget: TempFile,
  ): TempFile {
    val renderedServiceReport = getCompiledStyleTemplate(params, renderRequestInfo)
    val compiledStyleTemplate = getCompiledStyleTemplate()

    BufferedWriter(outputTarget.getWriter()).use { writer ->
      compiledStyleTemplate.execute(
        writer,
        mapOf("serviceTemplate" to renderedServiceReport),
      ).flush()
    }
    return outputTarget
  }

  fun getCompiledStyleTemplate(params: RenderParameters, renderRequestInfo: RenderRequestInfo): String {
    val handlebars = Handlebars()
    handlebars.registerHelpers(templateHelpers)
    val compiledServiceTemplate = handlebars.compileInline(params.template)

    val context = Context
      .newBuilder(params.data)
      .combine("render-request-info", renderRequestInfo)
      .build()

    return compiledServiceTemplate.apply(context)
  }

  private fun getCompiledStyleTemplate(): Mustache {
    val defaultMustacheFactory = DefaultMustacheFactory()
    val styleTemplate = getStyleTemplate()
    return defaultMustacheFactory.compile(StringReader(styleTemplate), "styleTemplate")
  }

  private fun getStyleTemplate() = this::class.java.getResource(STYLE_TEMPLATE_PATH)?.readText() ?: ""
}

data class RenderParameters(val templateVersion: String, val template: String, val data: Any?)

/**
 * Represents a temporary file used to store rendered SAR output.
 *
 * The underlying file is deleted when [close] is invoked. Callers should
 * use Kotlin's `use {}` block where possible to ensure the file is cleaned
 * up correctly.
 */
class TempFile(
  val path: Path,
) : AutoCloseable {

  companion object {
    private val log = LoggerFactory.getLogger(TempFile::class.java)
  }

  init {
    if (!Files.exists(path)) {
      log.info("temp file does not exists at path {}, creating it", path)
      Files.createFile(path)
    }
  }

  fun getWriter(): Writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)

  fun getInputStream(): InputStream = Files.newInputStream(path)

  internal fun getContentAsString(): String = Files.readString(path, UTF_8)

  override fun close() {
    log.info("close invoked on tempFile deleting file at path {}", path)
    Files.deleteIfExists(path)
  }
}
