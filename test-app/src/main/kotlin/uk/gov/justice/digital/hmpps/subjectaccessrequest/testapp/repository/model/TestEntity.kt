package uk.gov.justice.digital.hmpps.subjectaccessrequest.testapp.repository.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "test_app")
data class TestEntity(
  @Id
  val id: UUID = UUID.randomUUID(),
  val testName: String,
  val testLabel: String,
  val testDate: LocalDate,
  val testFlag: Boolean,
)
