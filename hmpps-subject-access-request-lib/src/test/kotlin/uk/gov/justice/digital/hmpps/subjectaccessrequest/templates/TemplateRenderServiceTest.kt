package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
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
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>String Equality: </td><td>true</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Nested Data: </td><td>nestedValue1</td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Array Data: </td><td><ul><li>arrayValue1-1</li><li>arrayValue1-2</li></ul></td></tr>")
      assertThat(generatedHtml).containsOnlyOnce("<td>Array Data Indexed: </td>")
      assertThat(generatedHtml).containsOnlyOnce("<li>Array data 1 - arrayValue1-1</li>")
      assertThat(generatedHtml).containsOnlyOnce("<li>Array data 2 - arrayValue1-2</li>")
      assertThat(generatedHtml).containsOnlyOnce("<tr><td>Formatted Date field: </td><td>26 July 2023, 12:59:57 pm</td></tr>")

      verify(templateDataFetcher, times(1)).findPrisonNameByPrisonId("AZ")
      verify(templateDataFetcher, times(1)).findUserLastNameByUsername("354703")
      verify(templateDataFetcher, times(1)).findLocationNameByDpsId("1234")
      verify(templateDataFetcher, times(1)).findLocationNameByNomisId(789)
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
    @NullSource
    @EmptySource
    @ValueSource(strings = ["  "])
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
    @NullSource
    @EmptySource
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
    @NullSource
    @EmptySource
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
        expectValue = "<tr><td>Boolean Value: </td><td>$expected</td></tr>",
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
        expectValue = "<tr><td>Camel Case: </td><td>$expected</td></tr>",
      )
    }
  }

  @Nested
  inner class Eq {

    @ParameterizedTest
    @CsvSource(
      value = [
        "         |         | true",
        " ''      | ''      | true",
        " ABC     | ABC     | true",
        "         | ''      | false",
        " 'Abc'   | 'ABC'   | false",
        " 'ABC '  | 'ABC'   | false",
      ],
      delimiter = '|',
    )
    fun `should return expected value when comparing strings for equality`(
      value1: String?,
      value2: String?,
      expected: Boolean,
    ) {
      assertContainsExpectedValueOnce(
        actual = renderReportHtml(TestServiceData(testKey = value1, testKey2 = value2)),
        expectValue = "<tr><td>String Equality: </td><td>$expected</td></tr>",
      )
    }
  }

  /**
   * See [DateConversionHelper].dateConversions for date patterns.
   */
  @Nested
  inner class FormatDate {

    // Date pattern 1 - "2024-05-01"
    @Test
    fun `should format date pattern 1 correctly`() {
      assertContainsExpectedFormattedDateValue(input = "2001-03-01", expected = "01 March 2001")
    }

    // Date pattern 2 - "01/05/2024"
    @Test
    fun `should format date pattern 2 correctly`() {
      assertContainsExpectedFormattedDateValue(input = "25/12/1999", expected = "25 December 1999")
    }

    // Date time pattern 3
    // "2024-05-01T12:34:56[.1|12|123|1234|12345|123456|1234567|12345678|123456789]"
    @ParameterizedTest
    @CsvSource(
      value = [
        "2025-01-01T12:34:56.1          | 01 January 2025, 12:34:56 pm",
        "2024-02-02T11:33:55.12         | 02 February 2024, 11:33:55 am",
        "2023-03-03T10:32:54.123        | 03 March 2023, 10:32:54 am",
        "2022-04-04T09:31:53.1234       | 04 April 2022, 9:31:53 am",
        "2021-05-05T13:30:52.12345      | 05 May 2021, 1:30:52 pm",
        "2020-06-06T14:29:51.123456     | 06 June 2020, 2:29:51 pm",
        "2019-07-07T15:28:50.123457     | 07 July 2019, 3:28:50 pm",
        "2018-08-08T16:27:49.1234578    | 08 August 2018, 4:27:49 pm",
        "2017-09-09T17:26:48.12345789   | 09 September 2017, 5:26:48 pm",
      ],
      delimiter = '|',
    )
    fun `should format date time pattern 3 correctly`(input: String?, expected: String) {
      assertContainsExpectedFormattedDateValue(input, expected)
    }

    // Date time pattern 4 - "2024-05-01T12:34:56[Z|+00:00|-00:00]"
    @ParameterizedTest
    @CsvSource(
      value = [
        // 1 digit precision
        "2025-01-01T12:34:56Z       | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56+01:00  | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56-01:00  | 01 January 2025, 1:34:56 pm",
      ],
      delimiter = '|',
    )
    fun `should format date time pattern 4 correctly`(input: String?, expected: String) {
      assertContainsExpectedFormattedDateValue(input, expected)
    }

    // Date time pattern 5 - "2024-05-01T12:34:56[.1|12|123|1234|12345|123456|1234567|12345678|123456789][Z|+00:00|-00:00]"
    @ParameterizedTest
    @CsvSource(
      value = [
        // 1 digit precision
        "2025-01-01T12:34:56.1Z               | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.1+01:00          | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.1-01:00          | 01 January 2025, 1:34:56 pm",
        // 2 digit precision
        "2025-01-01T12:34:56.12Z              | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.12+01:00         | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.12-01:00         | 01 January 2025, 1:34:56 pm",
        // 3 digit precision
        "2025-01-01T12:34:56.123Z             | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.123+01:00        | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.123-01:00        | 01 January 2025, 1:34:56 pm",
        // 4 digit precision
        "2025-01-01T12:34:56.1234Z            | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.1234+01:00       | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.1234-01:00       | 01 January 2025, 1:34:56 pm",
        // 5 digit precision
        "2025-01-01T12:34:56.12345Z           | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.12345+01:00      | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.12345-01:00      | 01 January 2025, 1:34:56 pm",
        // 6 digit precision
        "2025-01-01T12:34:56.123456Z          | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.123456+01:00     | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.123456-01:00     | 01 January 2025, 1:34:56 pm",
        // 7 digit precision
        "2025-01-01T12:34:56.1234567Z         | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.1234567+01:00    | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.1234567-01:00    | 01 January 2025, 1:34:56 pm",
        // 8 digit precision
        "2025-01-01T12:34:56.12345678Z        | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.12345678+01:00   | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.12345678-01:00   | 01 January 2025, 1:34:56 pm",
        // 9 digit precision
        "2025-01-01T12:34:56.123456789Z       | 01 January 2025, 12:34:56 pm",
        "2025-01-01T12:34:56.123456789+01:00  | 01 January 2025, 11:34:56 am",
        "2025-01-01T12:34:56.123456789-01:00  | 01 January 2025, 1:34:56 pm",
      ],
      delimiter = '|',
    )
    fun `should format date time pattern 5 correctly`(input: String?, expected: String) {
      assertContainsExpectedFormattedDateValue(input, expected)
    }

    // Date time pattern 6 - "01/05/2024 12:34
    @Test
    fun `should format date time pattern 6 correctly`() {
      assertContainsExpectedFormattedDateValue("01/05/2024 12:34", "01 May 2024, 12:34 pm")
    }

    // Date time pattern 7 - "01/05/2024 12:34:56
    @Test
    fun `should format date time pattern 7 correctly`() {
      assertContainsExpectedFormattedDateValue("01/05/2024 12:34:56", "01 May 2024, 12:34:56 pm")
    }

    // Date time pattern 8 - "2024-05-01 12:34:56[.1|12|123|1234|12345|123456][+00]"
    @ParameterizedTest
    @CsvSource(
      delimiter = '|',
      value = [
        "2026-01-01 12:00:00.1+00       | 01 January 2026, 12:00:00 pm",
        "2026-01-01 12:00:00.1+01       | 01 January 2026, 11:00:00 am",
        "2026-01-01 13:30:01.12+00      | 01 January 2026, 1:30:01 pm",
        "2026-01-01 13:30:01.12+01      | 01 January 2026, 12:30:01 pm",
        "2026-01-01 14:00:00.123+00     | 01 January 2026, 2:00:00 pm",
        "2026-01-01 14:00:00.123+01     | 01 January 2026, 1:00:00 pm",
        "2026-01-01 14:30:02.1234+00    | 01 January 2026, 2:30:02 pm",
        "2026-01-01 14:30:02.1234+01    | 01 January 2026, 1:30:02 pm",
        "2026-01-01 15:00:00.12345+00   | 01 January 2026, 3:00:00 pm",
        "2026-01-01 15:00:00.12345+01   | 01 January 2026, 2:00:00 pm",
        "2026-01-01 15:30:03.123456+00  | 01 January 2026, 3:30:03 pm",
        "2026-01-01 15:30:03.123456+01  | 01 January 2026, 2:30:03 pm",
      ],
    )
    fun `should format date time pattern 8 correctly`(input: String, expected: String) {
      assertContainsExpectedFormattedDateValue(input, expected)
    }

    // Date time pattern 9 - "2024-05-01T12:34"
    @Test
    fun `should format date time pattern 9 correctly`() {
      assertContainsExpectedFormattedDateValue("2024-05-01T12:34", "01 May 2024, 12:34 pm")
    }

    @Test
    fun `should format date time pattern 10 correctly`() {
      assertContainsExpectedFormattedDateValue("2024-05-01T12:34Z", "01 May 2024, 1:34 pm")
    }

    // Date time pattern 11 - 2025-04-03T14:34:41+0100
    @ParameterizedTest
    @CsvSource(
      delimiter = '|',
      value = [
        "2025-04-03T14:34:41+0100 | 03 April 2025, 2:34:41 pm",
        "2025-04-03T14:34:41-0100 | 03 April 2025, 4:34:41 pm",
        "2025-04-03T14:34:41+0000 | 03 April 2025, 3:34:41 pm",
        "2025-04-03T14:34:41-0000 | 03 April 2025, 3:34:41 pm",
      ],
    )
    fun `should format date time pattern 11 correctly`(input: String, expected: String) {
      assertContainsExpectedFormattedDateValue(input, expected)
    }

    private fun assertContainsExpectedFormattedDateValue(input: String?, expected: String) {
      assertContainsExpectedValueOnce(
        actual = renderReportHtml(TestServiceData(dateField = input)),
        expectValue = "<tr><td>Formatted Date field: </td><td>$expected</td></tr>",
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
  )

  private val testServiceData: List<TestServiceData> = listOf(
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
    ),
  )

  private fun ByteArrayOutputStream.toStringValue(): String = String(this.toByteArray())
}
