package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import kotlin.text.Charsets.UTF_8

class TemplateRenderServiceRegressionTest : BaseTemplateRenderServiceTest() {

  @TempDir
  lateinit var tempDir: Path

  private val legacyRenderService = TemplateRenderService(
    TemplateHelpers(
      templateDataFetcherFacade = templateDataFetcher,
      jacksonObjectMapper(),
    ),
  )

  private val fileBasedTemplateRenderService = FileBasedTemplateRenderService(
    TemplateHelpers(
      templateDataFetcherFacade = templateDataFetcher,
      jacksonObjectMapper(),
    ),
  )

  @Test
  fun `renderServiceTemplate and renderServiceTemplateToFile produce identical content`() {
    whenever(templateDataFetcher.findPrisonNameByPrisonId("AZ"))
      .thenReturn("Alcatraz")

    whenever(templateDataFetcher.findUserLastNameByUsername("354703"))
      .thenReturn("Capone")

    whenever(templateDataFetcher.findLocationNameByDpsId("1234"))
      .thenReturn("Cell 1234")

    whenever(templateDataFetcher.findLocationNameByNomisId(789))
      .thenReturn("Infirmary")

    whenever(templateDataFetcher.getRenderableAttachment(inlineAttachment, renderRequestInfo))
      .thenReturn("imagecontent".toByteArray(UTF_8))

    val renderParams = RenderParameters(
      templateVersion = "1.0",
      template = getResource("/templates/test-service-template.mustache"),
      data = testServiceData,
    )

    val actualInMemory = legacyRenderService.renderServiceTemplate(renderParams, renderRequestInfo)
    assertThat(actualInMemory).isNotNull

    TempFile(Files.createTempFile(tempDir, "report", ".html")).use { tempFile ->
      fileBasedTemplateRenderService.renderServiceTemplate(renderParams, renderRequestInfo, tempFile)

      assertThat(tempFile.path).exists()
      val actualFileOnDisk = tempFile.getContentAsString()

      assertThat(actualFileOnDisk).isNotNull
      assertThat(actualFileOnDisk).isEqualTo(actualInMemory.toStringValue())
    }
  }
}
