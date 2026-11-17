package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.attachments

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestTemplatingException
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.InlineAttachment
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateDataFetcherFacade
import java.util.Base64
import java.util.UUID
import java.util.stream.Stream

class EmbeddedInlineAttachmentsRendererTest {

  private val handlebars = Handlebars()
  private val renderRequestInfo = RenderRequestInfo(UUID.randomUUID(), "service-one")
  private val context = Context.newBuilder(
    mapOf(
      "attachment" to mapOf(
        "contentType" to "image/png",
        "url" to "http://url",
        "filesize" to 10,
      ),
    ),
  )
    .combine("render-request-info", renderRequestInfo)
    .build()

  private val options: Options = stubOptions(context)
  private val templateDataFetcherFacade: TemplateDataFetcherFacade = mock()


  private val embeddedInlineAttachmentRenderer = EmbeddedInlineAttachmentRenderer(
    templateDataFetcherFacade = templateDataFetcherFacade,
    objectMapper = jacksonObjectMapper(),
  )

  @Test
  fun `should return null when input is null`() {
    assertThat(embeddedInlineAttachmentRenderer.inlineAttachmentContent(null, options)).isNull()
    verifyNoInteractions(templateDataFetcherFacade)
  }

  @Test
  fun `should throw expected exception when input is not of type InlineAttachment`() {
    val notAttachment = listOf<Int>(1, 99, 21, 46)

    val actual = assertThrows<SubjectAccessRequestTemplatingException> {
      embeddedInlineAttachmentRenderer.inlineAttachment(
        input = notAttachment,
        width = null,
        height = null,
        options = options,
      )
    }

    assertThat(actual.message).startsWith("Could not convert object to inline attachment:")
    verifyNoInteractions(templateDataFetcherFacade)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "application/json",
      "application/pdf",
      "text/html",
      "video/mp4",
    ],
  )
  fun `should throw expected exception if inline attachment is not image`(contentType: String) {
    val attachment = mapOf(
      "contentType" to contentType,
      "url" to "http://url",
      "filesize" to 10,
    )

    val actual = assertThrows<SubjectAccessRequestTemplatingException> {
      embeddedInlineAttachmentRenderer.inlineAttachment(
        input = attachment,
        width = null,
        height = null,
        options = options,
      )
    }

    assertThat(actual.message).startsWith("Inline attachment with content type $contentType not supported")
    verifyNoInteractions(templateDataFetcherFacade)
  }

  @Test
  fun `should propagate exception throws by templateDataFetcherFacade getRenderableAttachment`() {
    val attachment = InlineAttachment(
      contentType = "image/png",
      url = "http://url",
      filesize = 10,
    )

    whenever(templateDataFetcherFacade.getRenderableAttachment(attachment, renderRequestInfo))
      .thenThrow(RuntimeException("Get attachment failed with some error"))

    val actual = assertThrows<RuntimeException> {
      embeddedInlineAttachmentRenderer.inlineAttachment(
        input = context["attachment"],
        width = null,
        height = null,
        options = options,
      )
    }

    assertThat(actual.message).isEqualTo("Get attachment failed with some error")

    verify(templateDataFetcherFacade, times(1))
      .getRenderableAttachment(attachment, renderRequestInfo)
  }

  @Test
  fun `should return embedded image data`() {
    val attachment = InlineAttachment(
      contentType = "image/png",
      url = "http://url",
      filesize = 10,
    )

    val imageData = "image attachment data here".toByteArray()
    val attachmentContent = "data:image/png;base64,${Base64.getEncoder().encodeToString(imageData)}"
    val expectedHtml = """<img src="$attachmentContent" alt="Inline attachment" height="200" width="200"/>"""

    whenever(templateDataFetcherFacade.getRenderableAttachment(attachment, renderRequestInfo))
      .thenReturn(imageData)

    val actual = embeddedInlineAttachmentRenderer.inlineAttachment(
      input = context["attachment"],
      width = 200,
      height = 200,
      options = options,
    )

    assertThat(actual).isEqualTo(expectedHtml)

    verify(templateDataFetcherFacade, times(1))
      .getRenderableAttachment(attachment, renderRequestInfo)
  }

  @Test
  fun `should return no data held when input is null`() {
    val input: Any? = null

    val context = Context.newBuilder(mapOf("attachment" to input))
      .combine("render-request-info", renderRequestInfo)
      .build()

    val options = stubOptions(context)

    val actual = embeddedInlineAttachmentRenderer.inlineAttachment(
      input = input,
      width = null,
      height = null,
      options = options,
    )

    assertThat(actual).isEqualTo("No Data Held")

    verifyNoInteractions(templateDataFetcherFacade)
  }

  @ParameterizedTest
  @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.attachments.EmbeddedInlineAttachmentsRendererTest#inlineAttachmentValues")
  fun `should handle supported content types`(
    attachment: InlineAttachment,
    attachmentData: ByteArray,
    width: Int?,
    height: Int?,
  ) {
    val context = Context.newBuilder(mapOf("attachment" to attachment.toMap()))
      .combine("render-request-info", renderRequestInfo)
      .build()

    val options = stubOptions(context)

    whenever(templateDataFetcherFacade.getRenderableAttachment(attachment, renderRequestInfo))
      .thenReturn(attachmentData)

    val actual = embeddedInlineAttachmentRenderer.inlineAttachment(
      input = attachment.toMap(),
      width = width,
      height = height,
      options = options,
    )

    val attachmentContent = "data:${attachment.contentType};base64,${attachmentData.toBase64Str()}"
    val expectedHtml = """<img src="$attachmentContent" alt="Inline attachment" height="$height" width="$width"/>"""
    assertThat(actual).isEqualTo(expectedHtml)
  }

  private fun stubOptions(context: Context): Options = Options(
    handlebars,
    "",
    mock(),
    context,
    mock(),
    mock(),
    emptyArray(),
    emptyMap(),
    emptyList(),
  )

  companion object {
    val attachmentBytes = "image attachment data here".toByteArray()

    @JvmStatic
    fun inlineAttachmentValues(): Stream<Arguments> = Stream.of(
      Arguments.of(InlineAttachment("image/jpeg", "http://url", 10), attachmentBytes, 10, 20),
      Arguments.of(InlineAttachment("image/png", "http://url", 100), attachmentBytes, null, null),
      Arguments.of(InlineAttachment("image/bmp", "http://url", 100), attachmentBytes, null, 100),
      Arguments.of(InlineAttachment("image/tiff", "http://url", 100), attachmentBytes, 101, null),
      Arguments.of(InlineAttachment("image/gif", "http://url", 100), attachmentBytes, 101, 200),
      Arguments.of(InlineAttachment("image/jpeg", "http://url", 100), attachmentBytes, 101, 200),
    )

    private fun InlineAttachment.toMap(): Map<String, Any?> = mapOf(
      "contentType" to this.contentType,
      "url" to this.url,
      "filesize" to this.filesize,
    )

    private fun ByteArray.toBase64Str(): String = Base64.getEncoder().encodeToString(this)

  }
}
