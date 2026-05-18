package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo

interface TemplateDataFetcherFacade {

  fun findPrisonNameByPrisonId(prisonId: String): String?
  fun findUserLastNameByUsername(userId: String): String?
  fun findLocationNameByNomisId(nomisId: Int): String?
  fun findLocationNameByDpsId(dpsId: String): String?
  fun getRenderableAttachment(attachment: InlineAttachment, renderRequestInfo: RenderRequestInfo): ByteArray
}

data class InlineAttachment(
  val contentType: String,
  val url: String,
  val filesize: Int? = null,
  val headers: List<InlineAttachmentHeader>? = null,
)

data class InlineAttachmentHeader(
  val name: String,
  val value: String,
)
