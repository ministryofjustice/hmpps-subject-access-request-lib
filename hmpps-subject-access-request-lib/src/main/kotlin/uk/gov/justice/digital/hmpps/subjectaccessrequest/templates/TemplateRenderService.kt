package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import uk.gov.justice.digital.hmpps.subjectaccessrequest.BaseTemplateRenderService
import uk.gov.justice.digital.hmpps.subjectaccessrequest.RenderParameters
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class TemplateRenderService(templateHelpers: TemplateHelpers) : BaseTemplateRenderService(templateHelpers) {

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
}
