package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

interface TemplateDataFetcherFacade {

  fun findPrisonNameByPrisonId(prisonId: String): String?
  fun findUserLastNameByUsername(userId: String): String?
  fun findLocationNameByNomisId(nomisId: Int): String?
  fun findLocationNameByDpsId(dpsId: String): String?
}
