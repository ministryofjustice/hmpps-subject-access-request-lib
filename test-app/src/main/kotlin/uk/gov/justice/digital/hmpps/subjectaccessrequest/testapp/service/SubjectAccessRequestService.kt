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
    content = SubjectAccessRequestResponse(
      testItems = listOf(
        TestItem(
          testName = "test-one",
          testLabel = "Test 1",
          testDate = LocalDate.parse("2026-02-01"),
          testFlag = true,
        ),
        TestItem(
          testName = "test-two",
          testLabel = "Test 2",
          testDate = LocalDate.parse("2026-03-01"),
          testFlag = false,
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

data class SubjectAccessRequestResponse(
  val testItems: List<TestItem>,
)

data class TestItem(
  val testName: String,
  val testLabel: String,
  val testDate: LocalDate,
  val testFlag: Boolean,
)
