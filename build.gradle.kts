plugins {
  kotlin("jvm") version "2.3.10"
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

kotlin {
  jvmToolchain(21)
}

allprojects {
  group = "uk.gov.justice.service.hmpps"
  version = "2.1.3"

  repositories {
    mavenLocal()
    mavenCentral()
  }
}

nexusPublishing {
  repositories {
    create("sonatype") {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      username.set(System.getenv("OSSRH_USERNAME"))
      password.set(System.getenv("OSSRH_PASSWORD"))
    }
  }
}