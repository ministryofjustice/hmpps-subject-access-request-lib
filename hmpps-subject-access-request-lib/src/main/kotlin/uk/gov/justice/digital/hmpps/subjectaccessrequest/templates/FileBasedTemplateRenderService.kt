package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.subjectaccessrequest.BaseTemplateRenderService
import uk.gov.justice.digital.hmpps.subjectaccessrequest.RenderParameters
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.BufferedWriter
import java.io.InputStream
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.text.Charsets.UTF_8

class FileBasedTemplateRenderService(templateHelpers: TemplateHelpers) : BaseTemplateRenderService(templateHelpers) {

  fun renderServiceTemplate(
    params: RenderParameters,
    renderRequestInfo: RenderRequestInfo,
    outputTarget: TempFile,
  ) {
    val renderedServiceReport = getCompiledStyleTemplate(params, renderRequestInfo)
    val compiledStyleTemplate = getCompiledStyleTemplate()

    BufferedWriter(outputTarget.getWriter()).use { writer ->
      compiledStyleTemplate.execute(writer, mapOf("serviceTemplate" to renderedServiceReport)).flush()
    }
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
