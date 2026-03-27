package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream

class TemplateRenderServiceTest {

  private val templateDataFetcher: TemplateDataFetcherFacade = mock()

  private val renderService = TemplateRenderService(
    TemplateHelpers(
      templateDataFetcherFacade = templateDataFetcher,
    ),
  )

  @Nested
  inner class RenderReportHtmlFromTemplateAndData {

    @Test
    fun `should render template and transform expected values`() {
      whenever(templateDataFetcher.findPrisonNameByPrisonId("AZ"))
        .thenReturn("Alcatraz")

      whenever(templateDataFetcher.findUserLastNameByUsername("354703"))
        .thenReturn("Capone")

      whenever(templateDataFetcher.findLocationNameByDpsId("1234"))
        .thenReturn("Cell 1234")

      whenever(templateDataFetcher.findLocationNameByNomisId(789))
        .thenReturn("Infirmary")

      val renderParams = RenderParameters(
        templateVersion = "1.0",
        template = getResource("/templates/test-service-template.mustache"),
        data = testServiceData,
      )

      val actual = renderService.renderServiceTemplate(renderParams)
      assertThat(actual).isNotNull

      val generatedHtml = actual.toStringValue()
      assertThat(generatedHtml).contains("<h2>Test Service Data</h2>")

      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Test Key:</td><td>testValue1</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Username: </td><td>Capone</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Prison: </td><td>Alcatraz</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>DPS Location: </td><td>Cell 1234</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Nomis Location: </td><td>Infirmary</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Boolean Value: </td><td>Yes</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Camel Case: </td><td>hello world</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Nested Data: </td><td>nestedValue1</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Array Data: </td><td><ul><li>arrayValue1-1</li><li>arrayValue1-2</li></ul></td></tr>")

      verify(templateDataFetcher, times(1)).findPrisonNameByPrisonId("AZ")
      verify(templateDataFetcher, times(1)).findUserLastNameByUsername("354703")
      verify(templateDataFetcher, times(1)).findLocationNameByDpsId("1234")
    }
  }

  @Nested
  inner class GetUserLastName {

    @Test
    fun `should render user last name for valid Id`() {
      whenever(templateDataFetcher.findUserLastNameByUsername("354703"))
        .thenReturn("Jailbird")

      val actual = renderReportHtml(TestServiceData(userId = "354703"))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Username: </td><td>Jailbird</td></tr>")
      verify(templateDataFetcher, times(1)).findUserLastNameByUsername("354703")
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "      | ",
        " ''   | ",
        " '  ' | ",
      ],
      delimiter = '|',
    )
    fun `should render no data held if id is null or empty`(id: String?) {
      val actual = renderReportHtml(TestServiceData(userId = id))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Username: </td><td>No Data Held</td></tr>")
      verify(templateDataFetcher, never()).findUserLastNameByUsername(anyString())
    }

    @Test
    fun `should return id if last name is null`() {
      whenever(templateDataFetcher.findUserLastNameByUsername("354703"))
        .thenReturn(null)

      val actual = renderReportHtml(TestServiceData(userId = "354703"))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Username: </td><td>354703</td></tr>")
      verify(templateDataFetcher, times(1)).findUserLastNameByUsername("354703")
    }
  }

  @Nested
  inner class GetPrisonName {

    @Test
    fun `should render prison name for valid Id`() {
      whenever(templateDataFetcher.findPrisonNameByPrisonId("AZ"))
        .thenReturn("Alcatraz")

      val actual = renderReportHtml(TestServiceData(prisonCode = "AZ"))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Prison: </td><td>Alcatraz</td></tr>")
      verify(templateDataFetcher, times(1)).findPrisonNameByPrisonId("AZ")
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "      | ",
        " ''   | ",
      ],
      delimiter = '|',
    )
    fun `should render no data held if id is null or empty`(id: String?) {
      val actual = renderReportHtml(TestServiceData(prisonCode = id))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Prison: </td><td>No Data Held</td></tr>")
      verify(templateDataFetcher, never()).findPrisonNameByPrisonId(anyString())
    }

    @Test
    fun `should return id if prison name is null`() {
      whenever(templateDataFetcher.findPrisonNameByPrisonId("AZ"))
        .thenReturn(null)

      val actual = renderReportHtml(TestServiceData(prisonCode = "AZ"))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Prison: </td><td>AZ</td></tr>")
      verify(templateDataFetcher, times(1)).findPrisonNameByPrisonId("AZ")
    }
  }

  @Nested
  inner class GetLocationNameByDpsId {

    @Test
    fun `should render location name for valid Id`() {
      whenever(templateDataFetcher.findLocationNameByDpsId("1234"))
        .thenReturn("Cell 666")

      val actual = renderReportHtml(TestServiceData(dpsLocationId = "1234"))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>DPS Location: </td><td>Cell 666</td></tr>")
      verify(templateDataFetcher, times(1)).findLocationNameByDpsId("1234")
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "      | ",
        " ''   | ",
      ],
      delimiter = '|',
    )
    fun `should render no data held if id is null or empty`(id: String?) {
      val actual = renderReportHtml(TestServiceData(dpsLocationId = id))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>DPS Location: </td><td>No Data Held</td></tr>")
      verify(templateDataFetcher, never()).findLocationNameByDpsId(anyString())
    }

    @Test
    fun `should render location Id if location is null`() {
      whenever(templateDataFetcher.findLocationNameByDpsId("1234"))
        .thenReturn(null)

      val actual = renderReportHtml(TestServiceData(dpsLocationId = "1234"))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>DPS Location: </td><td>1234</td></tr>")
      verify(templateDataFetcher, times(1)).findLocationNameByDpsId("1234")
    }
  }

  @Nested
  inner class GetLocationNameByNomisId {

    @Test
    fun `should render location name for valid Id`() {
      whenever(templateDataFetcher.findLocationNameByNomisId(1234))
        .thenReturn("Cell 666")

      val actual = renderReportHtml(TestServiceData(nomisLocationId = 1234))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Nomis Location: </td><td>Cell 666</td></tr>")
      verify(templateDataFetcher, times(1)).findLocationNameByNomisId(1234)
    }

    @Test
    fun `should render no data held if location Id is null`() {
      val actual = renderReportHtml(TestServiceData(nomisLocationId = null))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Nomis Location: </td><td>No Data Held</td></tr>")
      verify(templateDataFetcher, never()).findLocationNameByNomisId(any())
    }

    @Test
    fun `should render location Id if location result is null`() {
      whenever(templateDataFetcher.findLocationNameByNomisId(1234))
        .thenReturn(null)

      val actual = renderReportHtml(TestServiceData(nomisLocationId = 1234))

      assertContainsExpectedValueOnce(actual, expectValue = "<tr><td>Nomis Location: </td><td>1234</td></tr>")
      verify(templateDataFetcher, times(1)).findLocationNameByNomisId(1234)
    }
  }

  @Nested
  inner class ConvertBoolean {

    @ParameterizedTest
    @CsvSource(
      value = [
        " true    | Yes",
        " false   | No",
        " 1       | Yes",
        " 0       | No",
        "         | No Data Held",
        " Yellow  | Yellow",
      ],
      delimiter = '|',
    )
    fun `should convert boolean field to expected value`(input: Any?, expected: String) {
      assertContainsExpectedValueOnce(
        actual = renderReportHtml(TestServiceData(booleanVal = input)),
        expectValue = "<tr><td>Boolean Value: </td><td>${expected}</td></tr>",
      )
    }
  }

  @Nested
  inner class ConvertCamelCase {

    @ParameterizedTest
    @CsvSource(
      value = [
        "               | No Data Held",
        " ''            | No Data Held",
        " 'Hello World' | Hello World",
        " 'HelloWorld'  | hello world",
      ],
      delimiter = '|',
    )
    fun `should convert field to expected camel case value`(input: String?, expected: String) {
      assertContainsExpectedValueOnce(
        actual = renderReportHtml(TestServiceData(camelCaseVal = input)),
        expectValue = "<tr><td>Camel Case: </td><td>${expected}</td></tr>",
      )
    }
  }

  private fun renderReportHtml(data: TestServiceData): ByteArrayOutputStream = renderService.renderServiceTemplate(
    RenderParameters(
      templateVersion = "1.0",
      template = getResource("/templates/test-service-template.mustache"),
      data = listOf(data),
    ),
  )

  private fun assertContainsExpectedValueOnce(actual: ByteArrayOutputStream, expectValue: String) {
    assertThat(actual).isNotNull
    assertThat(actual.toStringValue()).containsOnlyOnce(expectValue)
  }


  private fun getResource(path: String): String = this::class.java.getResource(path)
    ?.readText()
    ?: fail("Resource $path not found")

  private data class TestServiceData(
    val testKey: String? = null,
    val prisonCode: String? = null,
    val dpsLocationId: String? = null,
    val nomisLocationId: Int? = null,
    val booleanVal: Any? = null,
    val camelCaseVal: String? = null,
    val userId: String? = null,
    val moreData: Map<String, Any> = emptyMap(),
    val arrayData: MutableList<String> = mutableListOf(),
  )

  private val testServiceData: List<TestServiceData> = listOf(
    TestServiceData(
      testKey = "testValue1",
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
    ),
  )

  private fun ByteArrayOutputStream.toStringValue(): String = String(this.toByteArray())
}