package uk.gov.justice.digital.hmpps.subjectaccessrequest.templates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

class TempFileTest {

  @TempDir
  lateinit var baseDir: Path

  @Test
  fun `should create expected file`() {
    TempFile(baseDir.resolve("report.html")).use { tempFile ->
      assertThat(tempFile.path).exists()
    }
  }

  @Test
  fun `should invoke AutoCloseable when used inside use block`() {
    val filePath = baseDir.resolve("report.html")
    TempFile(filePath).use { assertThat(filePath).exists() }

    assertThat(filePath).doesNotExist()
  }

  @Test
  fun `should delete file when close is called  `() {
    val filePath = baseDir.resolve("report.html")

    val tempFile = TempFile(filePath)
    assertThat(filePath).exists()

    tempFile.close()
    assertThat(filePath).doesNotExist()
  }

  @Test
  fun `should write content to expected file`() {
    val tempFile = TempFile(baseDir.resolve("report.html"))

    tempFile.getWriter().use { writer ->
      writer.write("Hello world!")
      writer.flush()
    }

    assertThat(Files.readString(tempFile.path)).isEqualTo("Hello world!")
  }

  @Test
  fun `getContentAsString returns the expected value`() {
    val tempFile = TempFile(baseDir.resolve("report.html"))

    tempFile.getWriter().use { writer ->
      writer.write("Hello world!")
      writer.flush()
    }

    assertThat(tempFile.getContentAsString()).isEqualTo("Hello world!")
  }

  @Test
  fun `getContentAsString returns the expected value when file is empty`() {
    val tempFile = TempFile(baseDir.resolve("report.html"))
    assertThat(tempFile.getContentAsString()).isEqualTo("")
  }

  @Test
  fun `getInputStream returns an inputStream to the expected file`() {
    val tempFile = TempFile(baseDir.resolve("report.html"))

    tempFile.getWriter().use { writer ->
      writer.write("SAR content here")
      writer.flush()
    }

    BufferedReader(InputStreamReader(tempFile.getInputStream())).use { reader ->
      assertThat(reader.readText()).isEqualTo("SAR content here")
    }
  }
}
