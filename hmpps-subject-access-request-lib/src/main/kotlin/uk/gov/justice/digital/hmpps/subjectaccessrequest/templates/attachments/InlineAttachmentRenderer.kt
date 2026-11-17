package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.attachments

import com.github.jknack.handlebars.Options

interface InlineAttachmentRenderer {

  fun inlineAttachmentContent(input: Any?, options: Options): String?

  fun inlineAttachment(input: Any?, height: Int? = null, width: Int? = null, options: Options): String
}