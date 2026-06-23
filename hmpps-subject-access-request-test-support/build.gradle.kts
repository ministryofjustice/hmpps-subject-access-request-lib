plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.7.0"
  kotlin("plugin.spring") version "2.3.20"
  kotlin("jvm") version "2.3.10"
  id("maven-publish")
  id("signing")
}

group = rootProject.group
version = rootProject.version.toString()

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation(project(":hmpps-subject-access-request-lib"))
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
  implementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
  implementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.2")
  implementation("org.flywaydb:flyway-core")
  implementation("jakarta.persistence:jakarta.persistence-api")
  implementation("org.jsoup:jsoup:1.22.2")
  implementation("com.helger:ph-css:8.2.1")
  implementation("com.itextpdf:itext7-core:9.6.0")
  implementation("com.itextpdf:html2pdf:6.3.2")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

publishing {
  repositories {
    mavenLocal()
  }
  publications {
    create<MavenPublication>("testSupportLibrary") {
      from(components["java"])
      pom {
        name.set(base.archivesName)
        artifactId = base.archivesName.get()
        description.set("A helper library to share code for use in testing subject access requests")
        url.set("https://github.com/ministryofjustice/hmpps-subject-access-request-lib")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("indyv-moj")
            name.set("Inderjit Virdi")
            email.set("inderjit.virdi@justice.gov.uk")
          }
        }
        scm {
          url.set("https://github.com/ministryofjustice/hmpps-subject-access-request-lib")
        }
      }
    }
  }
}

tasks.withType<PublishToMavenLocal> {
  signing {
    setRequired { false }
  }
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(publishing.publications["testSupportLibrary"])
}
java.sourceCompatibility = JavaVersion.VERSION_21

tasks.bootJar {
  enabled = false
}

tasks.jar {
  enabled = true
}

repositories {
  mavenLocal()
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
