package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.HandlebarsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestTemplatingException
import uk.gov.justice.digital.hmpps.subjectaccessrequest.rendering.RenderRequestInfo
import java.util.UUID
import java.util.stream.Stream
import kotlin.text.Charsets.UTF_8

private const val LOCATION_DPS_ID = "28953d06-d379-450c-9ec4-b5993ce5cd4f"
private const val LOCATION_NOMIS_ID = 4324567

class TemplateHelpersTest {

  private val templateDataFetcherFacade: TemplateDataFetcherFacade = mock()
  private val templateHelpers = TemplateHelpers(templateDataFetcherFacade, jacksonObjectMapper())

  @Nested
  inner class GetElementNumberTest {
    @Test
    fun `getElementNumber returns element number plus 1`() {
      val response = templateHelpers.getIndexPlusOne(1)
      assertThat(response).isEqualTo(2)
    }

    @Test
    fun `getElementNumber returns null given null`() {
      val response = templateHelpers.getIndexPlusOne(null)
      assertThat(response).isNull()
    }
  }

  @Nested
  inner class OptionalValueTest {
    @Test
    fun `optionalValue returns No Data Held if null`() {
      val response = templateHelpers.optionalValue(null)
      assertThat(response).isEqualTo("No Data Held")
    }

    @Test
    fun `optionalValue returns No Data Held if empty string`() {
      val response = templateHelpers.optionalValue("")
      assertThat(response).isEqualTo("No Data Held")
    }

    @Test
    fun `optionalValue returns input when not empty`() {
      val response = templateHelpers.optionalValue("BOB")
      assertThat(response).isEqualTo("BOB")
    }
  }

  @Nested
  inner class FormatDateTest {

    @Test
    fun `formatDate returns empty string if input is null`() {
      val response = templateHelpers.formatDate(null)
      assertThat(response).isEqualTo("")
    }

    @ParameterizedTest
    @CsvSource(
      value =
      [
        "2023-10-01, 01 October 2023",
        "2023-10-21T16:30:41Z, '21 October 2023, 5:30:41 pm'",
        "2023-01-21T16:30:41Z, '21 January 2023, 4:30:41 pm'",
        "2023-10-21T16:30:41+0000, '21 October 2023, 5:30:41 pm'",
        "2023-01-21T16:30:41+0000, '21 January 2023, 4:30:41 pm'",
        "2023-10-21T16:30:41+0100, '21 October 2023, 4:30:41 pm'",
        "2023-01-21T16:30:41+0100, '21 January 2023, 3:30:41 pm'",
        "2023-10-21T16:30:41-0300, '21 October 2023, 8:30:41 pm'",
        "2023-01-21T16:30:41-0300, '21 January 2023, 7:30:41 pm'",
        "2023-10-21T16:30:41+00:00, '21 October 2023, 5:30:41 pm'",
        "2023-01-21T16:30:41+00:00, '21 January 2023, 4:30:41 pm'",
        "2023-10-21T16:30:41+05:00, '21 October 2023, 12:30:41 pm'",
        "2023-01-21T16:30:41+05:00, '21 January 2023, 11:30:41 am'",
        "2023-10-21T16:30:41-04:00, '21 October 2023, 9:30:41 pm'",
        "2023-01-21T16:30:41-04:00, '21 January 2023, 8:30:41 pm'",
      ],
    )
    fun `formatDate returns formatted date for valid string input`(input: String, expected: String) {
      val response = templateHelpers.formatDate(input)
      assertThat(response).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpersTest#dateArrayValues")
    fun `formatDate returns formatted date for valid array input`(input: List<*>, expectedValue: String) {
      val response = templateHelpers.formatDate(input)
      assertThat(response).isEqualTo(expectedValue)
    }
  }

  @Nested
  inner class GetPrisonNameTest {
    @Test
    fun `getPrisonName returns prison name`() {
      whenever(templateDataFetcherFacade.findPrisonNameByPrisonId("MDI")).thenReturn("Moorland (HMP & YOI)")
      val response = templateHelpers.getPrisonName("MDI")
      assertThat(response).isEqualTo("Moorland (HMP & YOI)")
    }

    @Test
    fun `getPrisonName returns No Data Held if null`() {
      val response = templateHelpers.getPrisonName("")
      assertThat(response).isEqualTo("No Data Held")
    }
  }

  @Nested
  inner class GetUserLastNameTest {
    @Test
    fun `getUserLastName returns user last name`() {
      whenever(templateDataFetcherFacade.findUserLastNameByUsername("AQ987Z")).thenReturn("Johnson")
      val response = templateHelpers.getUserLastName("AQ987Z")
      assertThat(response).isEqualTo("Johnson")
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", " "])
    fun `getUserLastName returns No Data Held if null`(input: String?) {
      val response = templateHelpers.getUserLastName(input)
      assertThat(response).isEqualTo("No Data Held")
    }
  }

  @Nested
  inner class GetLocationNameByDpsIdTest {
    @Test
    fun `getLocationNameByDpsId returns location name from fetcher`() {
      whenever(templateDataFetcherFacade.findLocationNameByDpsId(LOCATION_DPS_ID)).thenReturn("PROPERTY BOX 27")
      val response = templateHelpers.getLocationNameByDpsId(LOCATION_DPS_ID)
      assertThat(response).isEqualTo("PROPERTY BOX 27")
    }

    @Test
    fun `getLocationNameByDpsId returns original id when not found from fetcher`() {
      whenever(templateDataFetcherFacade.findLocationNameByDpsId(LOCATION_DPS_ID)).thenReturn(null)
      val response = templateHelpers.getLocationNameByDpsId(LOCATION_DPS_ID)
      assertThat(response).isEqualTo(LOCATION_DPS_ID)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", " "])
    fun `getUserLastNameByDpsId returns No Data Held if null`(input: String?) {
      val response = templateHelpers.getLocationNameByDpsId(input)
      assertThat(response).isEqualTo("No Data Held")
    }
  }

  @Nested
  inner class GetLocationNameByNomisIdTest {
    @Test
    fun `getLocationNameByNomisId returns location name from fetcher`() {
      whenever(templateDataFetcherFacade.findLocationNameByNomisId(LOCATION_NOMIS_ID)).thenReturn("PROPERTY BOX 27")
      val response = templateHelpers.getLocationNameByNomisId(LOCATION_NOMIS_ID)
      assertThat(response).isEqualTo("PROPERTY BOX 27")
    }

    @Test
    fun `getLocationNameByNomisId returns original id when not found from fetcher`() {
      whenever(templateDataFetcherFacade.findLocationNameByNomisId(LOCATION_NOMIS_ID)).thenReturn(null)
      val response = templateHelpers.getLocationNameByNomisId(LOCATION_NOMIS_ID)
      assertThat(response).isEqualTo(LOCATION_NOMIS_ID.toString())
    }

    @ParameterizedTest
    @NullSource
    fun `getLocationNameByNomisId returns No Data Held if null`(input: Int?) {
      val response = templateHelpers.getLocationNameByNomisId(input)
      assertThat(response).isEqualTo("No Data Held")
    }
  }

  @Nested
  inner class ConvertBooleanTest {
    @ParameterizedTest
    @CsvSource(
      value = [
        "true  | Yes",
        "false | No",
        "      | No Data Held",
      ],
      delimiterString = "|",
    )
    fun `convertBoolean returns yes or no value when boolean`(inputValue: Boolean?, expectedValue: String) {
      val response = templateHelpers.convertBoolean(inputValue)
      assertThat(response).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "1    | Yes",
        "0    | No",
        "     | No Data Held",
      ],
      delimiterString = "|",
    )
    fun `convertBoolean returns yes or no value when 1 or 0`(inputValue: Integer?, expectedValue: String) {
      val response = templateHelpers.convertBoolean(inputValue)
      assertThat(response).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "true           | Yes",
        "false          | No",
        "1              | Yes",
        "0              | No",
        "Yes            | Yes",
        "No             | No",
        "               | No Data Held",
        "something else | something else",
      ],
      delimiterString = "|",
    )
    fun `convertBoolean returns original value when not boolean or 1 or 0`(inputValue: String?, expectedValue: String) {
      val response = templateHelpers.convertBoolean(inputValue)
      assertThat(response).isEqualTo(expectedValue)
    }
  }

  @Nested
  inner class BuildDateTest {
    @ParameterizedTest
    @CsvSource(
      value = [
        "2024 | 11    | null",
        "2024 | 11    | ",
        "2024 | null  | 15",
        "2024 |       | 15",
        "null | 11    | 15",
        "     | 11    | 15",
        "null | null  | null",
        "     |       |",
      ],
      delimiterString = "|",
      nullValues = ["null"],
    )
    fun `buildDate returns No Data Held when any blank input`(
      yearInput: String?,
      monthInput: String?,
      dayInput: String?,
    ) {
      val response = templateHelpers.buildDate(yearInput, monthInput, dayInput)
      assertThat(response).isEqualTo("No Data Held")
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "year     | 11    | 19        | year-11-19",
        "2024     | month | 19        | 2024-month-19",
        "2024     | 11    | something | 2024-11-something",
        "invalid  | date  | vals      | invalid-date-vals",
      ],
      delimiterString = "|",
    )
    fun `buildDate returns original values when not able to convert to date`(
      yearInput: String?,
      monthInput: String?,
      dayInput: String?,
      expectedValue: String,
    ) {
      val response = templateHelpers.buildDate(yearInput, monthInput, dayInput)
      assertThat(response).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "2024 | 11    | 19        | 19 November 2024",
        "2024 | 4     | 8         | 08 April 2024",
        "2024 | 04    | 08        | 08 April 2024",
      ],
      delimiterString = "|",
    )
    fun `buildDate returns formatted date`(
      yearInput: String?,
      monthInput: String?,
      dayInput: String?,
      expectedValue: String,
    ) {
      val response = templateHelpers.buildDate(yearInput, monthInput, dayInput)
      assertThat(response).isEqualTo(expectedValue)
    }
  }

  @Nested
  inner class BuildDateNumberTest {
    @ParameterizedTest
    @CsvSource(
      value = [
        "2024 | 11    | null",
        "2024 | null  | 15",
        "null | 11    | 15",
        "null | null  | null",
      ],
      delimiterString = "|",
      nullValues = ["null"],
    )
    fun `buildDateNumber returns No Data Held when any blank input`(
      yearInput: Integer?,
      monthInput: Integer?,
      dayInput: Integer?,
    ) {
      val response = templateHelpers.buildDateNumber(yearInput, monthInput, dayInput)
      assertThat(response).isEqualTo("No Data Held")
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "-102 | 11  | 19  | -102-11-19",
        "2024 | -11 | 19  | 2024--11-19",
        "2024 | 11  | -19 | 2024-11--19",
      ],
      delimiterString = "|",
    )
    fun `buildDateNumber returns original values when not able to convert to date`(
      yearInput: Integer?,
      monthInput: Integer?,
      dayInput: Integer?,
      expectedValue: String,
    ) {
      val response = templateHelpers.buildDateNumber(yearInput, monthInput, dayInput)
      assertThat(response).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "2024     | 11     | 19     | 19 November 2024",
        "2024     | 4      | 8      | 08 April 2024",
        "2024     | 04     | 08     | 08 April 2024",
        "2024.0   | 04.0   | 08.0   | 08 April 2024",
        "2024.434 | 04.563 | 08.544 | 08 April 2024",
      ],
      delimiterString = "|",
    )
    fun `buildDateNumber returns formatted date`(
      yearInput: Double?,
      monthInput: Double?,
      dayInput: Double?,
      expectedValue: String,
    ) {
      val response = templateHelpers.buildDateNumber(yearInput, monthInput, dayInput)
      assertThat(response).isEqualTo(expectedValue)
    }
  }

  @Nested
  inner class EqualsTest {
    @ParameterizedTest
    @CsvSource(
      value = [
        "Value 1 | Value 1",
        "null    | null",
      ],
      delimiterString = "|",
      nullValues = ["null"],
    )
    fun `eq returns true when args are equal`(firstArg: String?, secondArg: String?) {
      val response = templateHelpers.eq(firstArg, secondArg)
      assertThat(response).isTrue()
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "Value 1 | Value 2",
        "null    | Value 2",
        "Value 1 | null",
      ],
      delimiterString = "|",
      nullValues = ["null"],
    )
    fun `eq returns false when args are not equal`(firstArg: String?, secondArg: String?) {
      val response = templateHelpers.eq(firstArg, secondArg)
      assertThat(response).isFalse()
    }
  }

  @Nested
  inner class ConvertCamelCaseTest {
    @ParameterizedTest
    @CsvSource(
      value = [
        "camelCaseValue       | camel case value",
        "SomeValue            | some value",
        "Value With Spaces    | Value With Spaces",
        "                     | No Data Held",
        "null                 | No Data Held",
      ],
      delimiterString = "|",
      nullValues = ["null"],
    )
    fun `convertCamelCase returns expected value`(input: String?, expectedValue: String) {
      val response = templateHelpers.convertCamelCase(input)
      assertThat(response).isEqualTo(expectedValue)
    }
  }

  @Nested
  inner class OptionalStringTest {
    @ParameterizedTest
    @CsvSource(
      value = [
        "Data is Held       | Data is Held",
        "SOme random String | SOme random String",
        "''                 | No Data Held",
        "null               | No Data Held",
      ],
      delimiterString = "|",
      nullValues = ["null"],
    )
    fun `should return expected value`(input: String?, expectedValue: String) {
      assertThat(templateHelpers.optionalString(input)).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpersTest#nonStringValues")
    fun `should throw expected exception if input is no String`(value: Any) {
      val actual = assertThrows<SubjectAccessRequestTemplatingException> {
        templateHelpers.optionalString(value)
      }
      assertThat(actual.message).startsWith("required type String or null, but actual type was ${value::class.simpleName}")
    }
  }

  @Nested
  inner class InlineAttachmentTest {
    private val handlebars = Handlebars()
    private val renderRequestInfo = RenderRequestInfo(UUID.randomUUID(), "service-one")

    @BeforeEach
    fun setUp() {
      handlebars.registerHelpers(templateHelpers)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpersTest#inlineAttachmentValues")
    fun `should return expected value`(input: Map<String, Any>?, height: Int?, width: Int?, expectedValue: String?) {
      whenever(
        templateDataFetcherFacade.getRenderableAttachment(
          any(),
          eq(renderRequestInfo),
        ),
      ).thenReturn("filecontent".toByteArray(UTF_8))
      val context =
        Context.newBuilder(mapOf("attachment" to input)).combine("render-request-info", renderRequestInfo).build()
      val template = handlebars.compileInline("{{{inlineAttachment attachment $height $width}}}")

      val result = template.apply(context)

      assertThat(result).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpersTest#inlineAttachmentInvalidValues")
    fun `should throw expected exception if input is not correct map`(input: Map<String, Any>) {
      val context =
        Context.newBuilder(mapOf("attachment" to input)).combine("render-request-info", renderRequestInfo).build()
      val template = handlebars.compileInline("{{{inlineAttachment attachment}}}")

      val actual = assertThrows<HandlebarsException> { template.apply(context) }

      assertThat(actual.cause).isInstanceOf(SubjectAccessRequestTemplatingException::class.java)
        .hasMessageStartingWith("Could not convert object to inline attachment")
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
      val context = Context.newBuilder(
        mapOf(
          "attachment" to mapOf(
            "contentType" to contentType,
            "url" to "http://url",
            "filesize" to 10,
          ),
        ),
      ).combine("render-request-info", renderRequestInfo).build()
      val template = handlebars.compileInline("{{{inlineAttachment attachment}}}")

      val actual = assertThrows<HandlebarsException> { template.apply(context) }

      assertThat(actual.cause).isInstanceOf(SubjectAccessRequestTemplatingException::class.java)
        .hasMessageStartingWith("Inline attachment with content type $contentType not supported")
    }
  }

  @Nested
  inner class InlineAttachmentContentTest {
    private val handlebars = Handlebars()
    private val renderRequestInfo = RenderRequestInfo(UUID.randomUUID(), "service-one")

    @BeforeEach
    fun setUp() {
      handlebars.registerHelpers(templateHelpers)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpersTest#inlineAttachmentValues")
    fun `should return expected value`(input: Map<String, Any>?, height: Int?, width: Int?, expectedValue: String?) {
      whenever(
        templateDataFetcherFacade.getRenderableAttachment(
          any(),
          eq(renderRequestInfo),
        ),
      ).thenReturn("filecontent".toByteArray(UTF_8))
      val context =
        Context.newBuilder(mapOf("attachment" to input)).combine("render-request-info", renderRequestInfo).build()
      val template = handlebars.compileInline(
        """
        {{#attachment}}
          <img src="{{{inlineAttachmentContent attachment}}}" alt="Inline attachment" height="$height" width="$width"/>
        {{/attachment}}
        {{^attachment}}
          No Data Held
        {{/attachment}}""",
      )

      val result = template.apply(context)

      assertThat(result.trim()).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.TemplateHelpersTest#inlineAttachmentInvalidValues")
    fun `should throw expected exception if input is not correct map`(input: Map<String, Any>) {
      val context =
        Context.newBuilder(mapOf("attachment" to input)).combine("render-request-info", renderRequestInfo).build()
      val template = handlebars.compileInline("{{inlineAttachmentContent attachment}}")

      val actual = assertThrows<HandlebarsException> { template.apply(context) }

      assertThat(actual.cause).isInstanceOf(SubjectAccessRequestTemplatingException::class.java)
        .hasMessageStartingWith("Could not convert object to inline attachment")
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
      val context = Context.newBuilder(
        mapOf(
          "attachment" to mapOf(
            "contentType" to contentType,
            "url" to "http://url",
            "filesize" to 10,
          ),
        ),
      ).combine("render-request-info", renderRequestInfo).build()
      val template = handlebars.compileInline("{{inlineAttachmentContent attachment}}")

      val actual = assertThrows<HandlebarsException> { template.apply(context) }

      assertThat(actual.cause).isInstanceOf(SubjectAccessRequestTemplatingException::class.java)
        .hasMessageStartingWith("Inline attachment with content type $contentType not supported")
    }
  }

  companion object {
    @JvmStatic
    fun dateArrayValues(): Stream<Arguments> = Stream.of(
      Arguments.of(listOf(2023, 3, 24, 13, 59, 16, 133644), "24 March 2023, 1:59:16 pm"),
      Arguments.of(listOf(2023, 3, 24, 13, 59, 16), "24 March 2023, 1:59:16 pm"),
      Arguments.of(listOf(2023, 3, 24, 13, 59), "24 March 2023, 1:59 pm"),
      Arguments.of(listOf(2023, 3, 24, 13), "24 March 2023, 1:00 pm"),
      Arguments.of(listOf(2023, 3, 24), "24 March 2023, 12:00 am"),
      Arguments.of(listOf(2023, 3), "01 March 2023, 12:00 am"),
      Arguments.of(listOf(2023), "01 January 2023, 12:00 am"),
      Arguments.of(emptyList<Int>(), "01 January 0001, 12:00 am"),
      Arguments.of(listOf(2023.0, 3.0, 24.0, 13.0, 59.0, 16.0, 133644.0), "24 March 2023, 1:59:16 pm"),
      Arguments.of(listOf(2023.0, 3.0, 24.0, 13.0, 59.0, 16.0), "24 March 2023, 1:59:16 pm"),
      Arguments.of(listOf(2023.0, 3.0, 24.0, 13.0, 59.0), "24 March 2023, 1:59 pm"),
      Arguments.of(listOf(2023.0, 3.0, 24.0, 13.0), "24 March 2023, 1:00 pm"),
      Arguments.of(listOf(2023.0, 3.0, 24.0), "24 March 2023, 12:00 am"),
      Arguments.of(listOf(2023.0, 3.0), "01 March 2023, 12:00 am"),
      Arguments.of(listOf(2023.0), "01 January 2023, 12:00 am"),
      Arguments.of(listOf("2023", "3", "24", "13", "59", "16", "133644"), "24 March 2023, 1:59:16 pm"),
      Arguments.of(listOf("2023", "3", "24", "13", "59", "16"), "24 March 2023, 1:59:16 pm"),
      Arguments.of(listOf("2023", "3", "24", "13", "59"), "24 March 2023, 1:59 pm"),
      Arguments.of(listOf("2023", "3", "24", "13"), "24 March 2023, 1:00 pm"),
      Arguments.of(listOf("2023", "3", "24"), "24 March 2023, 12:00 am"),
      Arguments.of(listOf("2023", "3"), "01 March 2023, 12:00 am"),
      Arguments.of(listOf("2023"), "01 January 2023, 12:00 am"),
    )

    @JvmStatic
    fun nonStringValues(): List<Any> = listOf(
      99,
      10.0,
      mutableMapOf("A" to "B"),
      object {
        val name: String = "Homer Simpson"
      },
      true,
      false,
      listOf("1", "2", "3"),
    )

    @JvmStatic
    fun inlineAttachmentValues(): Stream<Arguments> = Stream.of(
      Arguments.of(null, null, null, "No Data Held"),
      Arguments.of(mapOf("contentType" to "image/jpeg", "url" to "http://url", "filesize" to 10), null, null, "<img src=\"data:image/jpeg;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"null\" width=\"null\"/>"),
      Arguments.of(mapOf("contentType" to "image/png", "url" to "http://url", "filesize" to 10), null, null, "<img src=\"data:image/png;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"null\" width=\"null\"/>"),
      Arguments.of(mapOf("contentType" to "image/bmp", "url" to "http://url", "filesize" to 10), null, null, "<img src=\"data:image/bmp;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"null\" width=\"null\"/>"),
      Arguments.of(mapOf("contentType" to "image/tiff", "url" to "http://url", "filesize" to 10), null, null, "<img src=\"data:image/tiff;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"null\" width=\"null\"/>"),
      Arguments.of(mapOf("contentType" to "image/gif", "url" to "http://url", "filesize" to 10, "headers" to listOf(mapOf("name" to "header-one", "value" to "val123"))), null, null, "<img src=\"data:image/gif;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"null\" width=\"null\"/>"),
      Arguments.of(mapOf("contentType" to "image/jpeg", "url" to "http://url", "filesize" to 10), 50, null, "<img src=\"data:image/jpeg;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"50\" width=\"null\"/>"),
      Arguments.of(mapOf("contentType" to "image/jpeg", "url" to "http://url", "filesize" to 10), null, 50, "<img src=\"data:image/jpeg;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"null\" width=\"50\"/>"),
      Arguments.of(mapOf("contentType" to "image/jpeg", "url" to "http://url", "filesize" to 10), 100, 75, "<img src=\"data:image/jpeg;base64,ZmlsZWNvbnRlbnQ=\" alt=\"Inline attachment\" height=\"100\" width=\"75\"/>"),
    )

    @JvmStatic
    fun inlineAttachmentContentValues(): Stream<Arguments> = Stream.of(
      Arguments.of(null, null),
      Arguments.of(mapOf("contentType" to "image/jpeg", "url" to "http://url", "filesize" to 10), "data:image/jpeg;base64,ZmlsZWNvbnRlbnQ="),
      Arguments.of(mapOf("contentType" to "image/png", "url" to "http://url", "filesize" to 10), "data:image/png;base64,ZmlsZWNvbnRlbnQ="),
      Arguments.of(mapOf("contentType" to "image/bmp", "url" to "http://url", "filesize" to 10), "data:image/bmp;base64,ZmlsZWNvbnRlbnQ="),
      Arguments.of(mapOf("contentType" to "image/tiff", "url" to "http://url", "filesize" to 10), "data:image/tiff;base64,ZmlsZWNvbnRlbnQ="),
      Arguments.of(mapOf("contentType" to "image/gif", "url" to "http://url", "filesize" to 10, "headers" to listOf(mapOf("name" to "header-one", "value" to "val123"))), "data:image/gif;base64,ZmlsZWNvbnRlbnQ="),
    )

    @JvmStatic
    fun inlineAttachmentInvalidValues(): List<Map<String, Any>?> = listOf(
      mapOf("contentType" to "image/jpeg"),
      mapOf("url" to "http://url", "filesize" to 10),
    )
  }
}
