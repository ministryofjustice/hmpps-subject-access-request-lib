package uk.gov.justice.digital.hmpps.subjectaccessrequest.testapp.service

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.kotlin.sar.Attachment
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent = HmppsSubjectAccessRequestContent(
    content = SubjectAccessRequestContent(
      testItems = listOf(
        TestItem(
          testName = "test-one",
          testLabel = "Test 1",
          testDate = LocalDate.parse("2026-02-01"),
          testFlag = true,
          attachment = InlineAttachment(
            contentType = "image/png",
            url = "http://image.png",
            filesize = 12345,
            headers = listOf(InlineAttachmentHeader(name = "Test-Header", value = "123abc")),
          ),
        ),
        TestItem(
          testName = "test-two",
          testLabel = "Test 2",
          testDate = LocalDate.parse("2026-03-01"),
          testFlag = false,
          attachment = InlineAttachment(
            contentType = "image/jpeg",
            url = "http://map.jpg",
            headers = listOf(InlineAttachmentHeader(name = "Test-Header", value = "456def")),
          ),
        ),
        TestItem(
          testName = "test-three",
          testLabel = "Test 3",
          testDate = LocalDate.parse("2026-04-01"),
          testFlag = true,
        ),
      ),
    ),
    attachments = listOf(
      Attachment(
        attachmentNumber = 1,
        name = "Attachment 1",
        contentType = "application/pdf",
        url = "http://url",
        filesize = 100,
        filename = "attachment.pdf",
      ),
    ),
  )
}

data class SubjectAccessRequestContent(
  val testItems: List<TestItem>,
)

data class TestItem(
  val testName: String,
  val testLabel: String,
  val testDate: LocalDate,
  val testFlag: Boolean,
  val attachment: InlineAttachment? = null,
)

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
