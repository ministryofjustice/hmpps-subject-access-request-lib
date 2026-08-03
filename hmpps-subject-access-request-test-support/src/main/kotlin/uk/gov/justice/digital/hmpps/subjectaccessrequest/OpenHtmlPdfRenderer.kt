package uk.gov.justice.digital.hmpps.subjectaccessrequest

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.layout.Document
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.jsoup.nodes.Document as JsoupDocument

class OpenHtmlPdfRenderer : PdfRenderer {
  override fun renderSubjectAccessRequestPdf(
    serviceHtml: String,
    prn: String?,
    crn: String?,
  ): ByteArrayOutputStream {
    val xhtml = buildXhtmlDocument(serviceHtml = serviceHtml)
    val contentOutputStream = ByteArrayOutputStream()

    PdfRendererBuilder()
      .useFastMode()
      .withHtmlContent(xhtml, null)
      .toStream(contentOutputStream)
      .run()

    val reportOutputStream = ByteArrayOutputStream()
    PdfDocument(PdfWriter(reportOutputStream)).use { pdfDocument ->
      val document = Document(pdfDocument)
      document.setMargins(50F, 35F, 70F, 35F)
      pdfDocument.addSubjectAccessRequestCustomHandler(document, "Test Subject", prn, crn)

      PdfDocument(PdfReader(ByteArrayInputStream(contentOutputStream.toByteArray()))).use { contentPdf ->
        PdfMerger(pdfDocument).merge(contentPdf, 1, contentPdf.numberOfPages)
      }
    }

    return reportOutputStream
  }

  private fun buildXhtmlDocument(serviceHtml: String): String {
    val serviceFragment = Jsoup.parseBodyFragment(serviceHtml)

    serviceFragment.outputSettings()
      .syntax(JsoupDocument.OutputSettings.Syntax.xml)
      .escapeMode(Entities.EscapeMode.xhtml)
      .charset(Charsets.UTF_8)
      .prettyPrint(false)

    val serviceCss = serviceFragment
      .select("style")
      .joinToString("\n") { it.html() }

    serviceFragment.select("style").remove()

    val serviceBodyHtml = serviceFragment.body().html()

    return """
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8" />
          <style type="text/css">
          @page {
            size: A4;
            margin: 50pt 35pt 70pt 35pt;
          }
        
          body {
            margin: 0;
            font-family: Helvetica, Arial, sans-serif;
            font-size: 12pt;
            color: #0b0c0c;
          }
        
          img {
            max-width: 100%;
          }
        
          .page-break {
            page-break-before: always;
            break-before: page;
          }
        
          $serviceCss
        
          table {
            max-width: 100%;
            border-collapse: collapse;
          }
        
          table.summary-list,
          table.data-table {
            max-width: 100%;
            table-layout: fixed;
          }
        
          td,
          th {
            word-wrap: break-word;
            white-space: normal;
            vertical-align: top;
          }
        </style>
        </head>
        <body>
          <main>
            $serviceBodyHtml
          </main>
        </body>
      </html>
    """.trimIndent()
  }
}
