package uk.gov.justice.digital.hmpps.subjectaccessrequest.exception

class SubjectAccessRequestTemplateValidationException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)