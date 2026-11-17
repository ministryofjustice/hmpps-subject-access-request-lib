package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.Options
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.apache.commons.lang3.StringUtils.leftPad
import org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestTemplatingException
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.attachments.EmbeddedInlineAttachmentRenderer
import uk.gov.justice.digital.hmpps.subjectaccessrequest.templates.attachments.InlineAttachmentRenderer
import java.lang.String.format
import java.time.LocalDateTime
import java.time.temporal.Temporal

open class TemplateHelpers(
  private val templateDataFetcherFacade: TemplateDataFetcherFacade,
  private val objectMapper: ObjectMapper,
  private val inlineAttachmentRenderer: InlineAttachmentRenderer = EmbeddedInlineAttachmentRenderer(
    templateDataFetcherFacade,
    objectMapper,
  ),
) {
  companion object {
    const val NO_DATA_HELD = "No Data Held"
    val dateConversionHelper = DateConversionHelper()
  }

  open fun formatDate(input: Any?): String {
    if (input == null) return ""
    return when (input) {
      is String -> dateConversionHelper.convertDates(input)
      is Temporal -> dateConversionHelper.convertDates(input.toString())
      is List<*> -> dateConversionHelper.convertDates(
        LocalDateTime.of(
          input.getOrDefault(0, 1),
          input.getOrDefault(1, 1),
          input.getOrDefault(2, 1),
          input.getOrDefault(3),
          input.getOrDefault(4),
          input.getOrDefault(5),
        ).toString(),
      )

      else -> input.toString()
    }
  }

  open fun List<*>.getOrDefault(index: Int, default: Int = 0): Int {
    if (this.size > index) {
      val listValue = this[index]
      return when (listValue) {
        is Number -> listValue.toInt()
        is String -> listValue.toInt()
        else -> default
      }
    }
    return default
  }

  open fun optionalValue(input: Any?, fallback: String? = null): Any {
    if (input == null || input == "") return fallback ?: NO_DATA_HELD
    return input
  }

  open fun optionalString(input: Any?, fallback: String? = null): Any {
    if (input == null) return fallback ?: NO_DATA_HELD

    if (input is String) {
      return if (isNotBlank(input)) input else fallback ?: NO_DATA_HELD
    }
    throw SubjectAccessRequestTemplatingException(
      message = "required type String or null, but actual type was ${input::class.simpleName}",
    )
  }

  open fun getIndexPlusOne(elementIndex: Int?): Int? {
    if (elementIndex != null) {
      return elementIndex + 1
    }
    return null
  }

  open fun getPrisonName(caseloadId: String?): String {
    if (caseloadId == null || caseloadId.isEmpty() || caseloadId == "") return NO_DATA_HELD
    val prisonName = templateDataFetcherFacade.findPrisonNameByPrisonId(caseloadId)

    return prisonName ?: caseloadId
  }

  fun getUserLastName(userId: String?): String {
    if (isBlank(userId)) return NO_DATA_HELD
    val userLastName = templateDataFetcherFacade.findUserLastNameByUsername(userId!!)

    return userLastName ?: userId
  }

  fun getLocationNameByDpsId(dpsId: String?): String = if (isBlank(dpsId)) {
    NO_DATA_HELD
  } else {
    templateDataFetcherFacade.findLocationNameByDpsId(dpsId!!) ?: dpsId
  }

  open fun getLocationNameByNomisId(nomisId: Int?): String = if (nomisId == null) {
    NO_DATA_HELD
  } else {
    templateDataFetcherFacade.findLocationNameByNomisId(nomisId) ?: nomisId.toString()
  }

  open fun convertBoolean(input: Any?): Any = when {
    input is Boolean && input -> "Yes"
    input is Boolean && !input -> "No"
    input == 1 || input == "1" || input == "true" -> "Yes"
    input == 0 || input == "0" || input == "false" -> "No"
    input == null -> NO_DATA_HELD
    else -> input
  }

  open fun buildDate(year: String?, month: String?, day: String?): String {
    if (isBlank(year) || isBlank(month) || isBlank(day)) {
      return NO_DATA_HELD
    }
    return formatDate(format("%s-%s-%s", leftPad(year, 4, "0"), leftPad(month, 2, "0"), leftPad(day, 2, "0")))
  }

  open fun buildDateNumber(year: Number?, month: Number?, day: Number?): String = buildDate(
    year?.toInt()?.toString(),
    month?.toInt()?.toString(),
    day?.toInt()?.toString(),
  )

  open fun eq(input: String?, value: String?): Boolean = input == value

  open fun convertCamelCase(input: String?): String {
    if (input == null || input == "") return NO_DATA_HELD
    if (input.contains(" ")) return input
    return splitByCharacterTypeCamelCase(input).joinToString(" ").lowercase()
  }

  open fun inlineAttachmentContent(
    input: Any?,
    options: Options,
  ): String? = inlineAttachmentRenderer.inlineAttachmentContent(input, options)

  open fun inlineAttachment(
    input: Any?,
    height: Int? = null,
    width: Int? = null,
    options: Options,
  ): String = inlineAttachmentRenderer.inlineAttachment(input, height, width, options)
}
