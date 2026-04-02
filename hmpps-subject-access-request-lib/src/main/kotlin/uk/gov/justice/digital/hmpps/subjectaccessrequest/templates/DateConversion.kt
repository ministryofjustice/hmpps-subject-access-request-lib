package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

class DateConversion(var matcher: Regex, var parseFormat: String, var outputFormat: String, val hasOffset: Boolean)
