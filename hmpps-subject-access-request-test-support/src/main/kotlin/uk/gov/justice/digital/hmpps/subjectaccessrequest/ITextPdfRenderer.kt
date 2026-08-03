package uk.gov.justice.digital.hmpps.subjectaccessrequest

import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.html2pdf.attach.impl.layout.HtmlPageBreak
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.IBlockElement
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.properties.AreaBreakType
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestException
import java.io.ByteArrayOutputStream

class ITextPdfRenderer : PdfRenderer {

  override fun renderSubjectAccessRequestPdf(serviceHtml: String, prn: String?, crn: String?): ByteArrayOutputStream {
    val bodyOutputStream = ByteArrayOutputStream()
    PdfDocument(PdfWriter(bodyOutputStream)).use { pdfDocument ->
      val document = Document(pdfDocument)
      document.setMargins(50F, 35F, 70F, 35F)
      pdfDocument.addSubjectAccessRequestCustomHandler(document, "Test Subject", prn, crn)
      document.addServiceData(serviceHtml)
    }
    return bodyOutputStream
  }

  private fun Document.addServiceData(serviceHtml: String) {
    HtmlConverter.convertToElements(serviceHtml).forEach { element ->
      when (element) {
        is IBlockElement -> this.add(element)
        is Image -> this.add(element)
        is HtmlPageBreak -> this.add(AreaBreak(AreaBreakType.NEXT_PAGE))
        else -> {
          throw SubjectAccessRequestException("Unsupported element type found ${element.javaClass}")
        }
      }
    }
  }
}
