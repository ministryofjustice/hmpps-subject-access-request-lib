package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.InputStream
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.text.Charsets.UTF_8

const val SAR_STYLE_SHEET_PATH = "/templates/main_stylesheet.html"

class FileBasedTemplateRenderService(private val templateHelpers: TemplateHelpers) {

  private val stylesheet by lazy {
    this::class.java
      .getResource(SAR_STYLE_SHEET_PATH)
      ?.readText()
      ?: error("Unable to load stylesheet $SAR_STYLE_SHEET_PATH")
  }

  fun renderServiceTemplate(
    params: RenderParameters,
    renderRequestInfo: RenderRequestInfo,
    outputTarget: TempFile,
  ) {
    val handlebars = Handlebars()
    handlebars.registerHelpers(templateHelpers)

    val serviceTemplate = handlebars.compileInline(getCombinedTemplate(params))

    val context = Context
      .newBuilder(params.data)
      .combine("render-request-info", renderRequestInfo)
      .build()

    outputTarget.getWriter().use { writer ->
      serviceTemplate.apply(context, writer)
    }
  }

  /**
   * Create a new combined template containing style sheet and the service template block.
   */
  private fun getCombinedTemplate(
    params: RenderParameters,
  ): String = buildString {
    append(stylesheet)
    appendLine()
    append(params.template)
  }
}

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
