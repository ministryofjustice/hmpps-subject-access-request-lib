package uk.gov.justice.digital.hmpps.subjectaccessrequest

import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.html2pdf.attach.impl.layout.HtmlPageBreak
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.IBlockElement
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.TextAlignment
import uk.gov.justice.digital.hmpps.subjectaccessrequest.exception.SubjectAccessRequestException
import java.io.ByteArrayOutputStream

class PdfRenderer {

  fun renderSubjectAccessRequestPdf(serviceHtml: String, prn: String?, crn: String?): ByteArrayOutputStream {
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

  private fun PdfDocument.addSubjectAccessRequestCustomHandler(document: Document, subjectName: String, prn: String?, crn: String?) {
    this.addEventHandler(PdfDocumentEvent.END_PAGE, CustomHeaderEventHandler(document, subjectName, prn, crn))
  }
}

class CustomHeaderEventHandler(val document: Document, private val subjectName: String, private val nomisId: String?, private val ndeliusCaseReferenceId: String?) : AbstractPdfDocumentEventHandler() {

  override fun onAcceptedEvent(currentEvent: AbstractPdfDocumentEvent) {
    val docEvent = currentEvent as PdfDocumentEvent
    val subjectIdLabel = nomisId?.let { "NOMIS ID: " } ?: ndeliusCaseReferenceId?.let { "nDelius ID: " } ?: ""
    val subjectIdValue = nomisId ?: ndeliusCaseReferenceId ?: ""
    val leftHeaderText = ""
    val rightHeaderText = Paragraph()
      .add(Text("Name: ").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)))
      .add(Text(subjectName).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)))
      .add(Text("\n"))
      .add(Text(subjectIdLabel).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)))
      .add(Text(subjectIdValue).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)))
    val font: PdfFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
    val pageSize = docEvent.page.pageSize
    val leftCoord = pageSize.left + document.leftMargin
    val rightCoord = pageSize.right - document.rightMargin
    val midCoord = (leftCoord + rightCoord) / 2
    val headerY: Float = pageSize.top - document.topMargin
    val footerY: Float = pageSize.bottom + 20
    val canvas = Canvas(docEvent.page, pageSize)
    canvas
      .setFont(font)
      .setFontSize(10f)
      .showTextAligned(
        leftHeaderText,
        leftCoord,
        headerY,
        TextAlignment.LEFT,
      ).simulateBold()
      .showTextAligned(
        rightHeaderText,
        rightCoord,
        headerY,
        TextAlignment.RIGHT,
      ).simulateBold()
      .showTextAligned(
        "Official Sensitive",
        midCoord,
        footerY,
        TextAlignment.CENTER,
      )
      .close()
  }
}
