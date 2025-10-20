plugins {
  kotlin("jvm") version "2.2.20"
  id("java-library")
  id("maven-publish")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(project(":hmpps-subject-access-request-lib"))
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.7.0")
  implementation("org.mockito.kotlin:mockito-kotlin:6.0.0")
  implementation("org.flywaydb:flyway-core")
  implementation("jakarta.persistence:jakarta.persistence-api")
}

publishing {
  publications {
    create<MavenPublication>("testSupportLibrary") {
      from(components["java"])
      pom {
        name.set(base.archivesName)
        artifactId = base.archivesName.get()
      }
    }
  }
}
