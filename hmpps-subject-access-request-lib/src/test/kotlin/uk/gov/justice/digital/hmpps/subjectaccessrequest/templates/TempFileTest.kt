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
    getTempFile().use { tempFile ->
      assertThat(tempFile.path).exists()
    }
  }

  @Test
  fun `should invoke AutoCloseable when used inside use block`() {
    val tempFile = getTempFile()
    tempFile.use { assertThat(tempFile.path).exists() }

    assertThat(tempFile.path).doesNotExist()
  }

  @Test
  fun `should delete file when close is called `() {
    val tempFile = getTempFile()
    val filepath = tempFile.path
    assertThat(filepath).exists()

    tempFile.close()
    assertThat(filepath).doesNotExist()
  }

  @Test
  fun `should write content to expected file`() {
    getTempFile().use { tempFile ->
      tempFile.getWriter().use { writer ->
        writer.write("Hello world!")
        writer.flush()
      }

      assertThat(Files.readString(tempFile.path)).isEqualTo("Hello world!")
    }
  }

  @Test
  fun `getContentAsString returns the expected value`() {
    getTempFile().use { tempFile ->
      tempFile.getWriter().use { writer ->
        writer.write("Hello world!")
        writer.flush()
      }

      assertThat(tempFile.getContentAsString()).isEqualTo("Hello world!")
    }
  }

  @Test
  fun `getContentAsString returns the expected value when file is empty`() {
    getTempFile().use { tempFile ->
      assertThat(tempFile.getContentAsString()).isEqualTo("")
    }
  }

  @Test
  fun `getInputStream returns an inputStream to the expected file`() {
    getTempFile().use { tempFile ->
      tempFile.getWriter().use { writer ->
        writer.write("SAR content here")
        writer.flush()
      }

      BufferedReader(InputStreamReader(tempFile.getInputStream())).use { reader ->
        assertThat(reader.readText()).isEqualTo("SAR content here")
      }
    }
  }

  private fun getTempFile(): TempFile = TempFile(Files.createTempFile(baseDir, "report", ".html"))
}
