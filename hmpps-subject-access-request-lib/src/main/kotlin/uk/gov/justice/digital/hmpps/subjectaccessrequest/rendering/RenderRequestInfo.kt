package uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering

import java.util.UUID

data class RenderRequestInfo(
  val id: UUID? = null,
  val serviceName: String,
)
