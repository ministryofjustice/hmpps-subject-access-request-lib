package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.attachments

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.Options
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestTemplatingException
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.InlineAttachment
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateDataFetcherFacade
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpers.Companion.NO_DATA_HELD
import java.util.Base64

class EmbeddedInlineAttachmentRenderer(
  private val templateDataFetcherFacade: TemplateDataFetcherFacade,
  private val objectMapper: ObjectMapper,
) : InlineAttachmentRenderer {

  override fun inlineAttachmentContent(
    input: Any?,
    options: Options,
  ): String? {
    if (input == null) {
      return null
    }

    val inlineAttachment = try {
      objectMapper.convertValue(input, InlineAttachment::class.java)
    } catch (e: IllegalArgumentException) {
      throw SubjectAccessRequestTemplatingException("Could not convert object to inline attachment: ${e.message}")
    }

    if (MediaType.parseMediaType(inlineAttachment.contentType).type != "image") {
      throw SubjectAccessRequestTemplatingException("Inline attachment with content type ${inlineAttachment.contentType} not supported")
    }

    val renderRequestInfo = options.context.get("render-request-info") as RenderRequestInfo
    val attachmentData = templateDataFetcherFacade.getRenderableAttachment(inlineAttachment, renderRequestInfo)
    val base64 = Base64.getEncoder().encodeToString(attachmentData)
    return "data:${inlineAttachment.contentType};base64,$base64"
  }

  override fun inlineAttachment(input: Any?, height: Int?, width: Int?, options: Options): String {
    if (input == null) {
      return NO_DATA_HELD
    }
    return """<img src="${inlineAttachmentContent(input, options)}" alt="Inline attachment" height="$height" width="$width"/>"""
  }
}
