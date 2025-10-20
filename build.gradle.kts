plugins {
  kotlin("jvm") version "2.2.20"
  id("maven-publish")
}

kotlin {
  jvmToolchain(21)
}

allprojects {
  group = "uk.gov.justice.digital.hmpps"
  version = "1.0.0"

  repositories {
    mavenLocal()
    mavenCentral()
  }
}
