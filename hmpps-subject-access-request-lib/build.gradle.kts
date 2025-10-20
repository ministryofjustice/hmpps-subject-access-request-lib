plugins {
  kotlin("jvm") version "2.2.20"
  id("java-library")
  id("maven-publish")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  api("org.apache.commons:commons-lang3:3.18.0")
  api("com.fasterxml.jackson.core:jackson-annotations:2.19.2")
  api("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.7.0")
  api("com.github.jknack:handlebars:4.4.0")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.7.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.0.0")
}

publishing {
  publications {
    create<MavenPublication>("mainLibrary") {
      from(components["java"])
      pom {
        name.set(base.archivesName)
        artifactId = base.archivesName.get()
      }
    }
  }
}
