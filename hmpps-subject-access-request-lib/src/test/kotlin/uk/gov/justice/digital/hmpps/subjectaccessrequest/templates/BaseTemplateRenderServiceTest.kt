package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import org.junit.jupiter.api.Assertions.fail
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.io.ByteArrayOutputStream
import java.util.UUID

abstract class BaseTemplateRenderServiceTest {

  protected val templateDataFetcher: TemplateDataFetcherFacade = mock()

  protected val inlineAttachment = InlineAttachment(
    contentType = "image/jpeg",
    "http://file-url",
    100,
  )

  protected val renderRequestInfo = RenderRequestInfo(UUID.randomUUID(), "service-one")

  protected fun getResource(path: String): String = this::class.java.getResource(path)
    ?.readText()
    ?: fail("Resource $path not found")

  protected data class TestServiceData(
    val testKey: String? = null,
    val testKey2: String? = null,
    val prisonCode: String? = null,
    val dpsLocationId: String? = null,
    val nomisLocationId: Int? = null,
    val booleanVal: Any? = null,
    val camelCaseVal: String? = null,
    val userId: String? = null,
    val moreData: Map<String, Any> = emptyMap(),
    val arrayData: MutableList<String> = mutableListOf(),
    val dateField: String? = null,
    val attachment: Map<String, Any>? = null,
  )

  protected val testServiceData: List<TestServiceData> = listOf(
    TestServiceData(
      testKey = "testValue1",
      testKey2 = "testValue1",
      prisonCode = "AZ",
      dpsLocationId = "1234",
      nomisLocationId = 789,
      booleanVal = true,
      camelCaseVal = "HelloWorld",
      userId = "354703",
      moreData = mapOf(
        "nestedKey" to "nestedValue1",
      ),
      arrayData = mutableListOf("arrayValue1-1", "arrayValue1-2"),
      dateField = "2023-07-26T12:59:57.961+01:00",
      attachment = mapOf(
        "contentType" to "image/jpeg",
        "url" to "http://file-url",
        "filesize" to 100,
      ),
    ),
  )

  protected fun ByteArrayOutputStream.toStringValue(): String = String(this.toByteArray())
}
