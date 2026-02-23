plugins {
  kotlin("jvm") version "2.2.20"
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

kotlin {
  jvmToolchain(25)
}

allprojects {
  group = "uk.gov.justice.service.hmpps"
  version = "2.0.2"

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