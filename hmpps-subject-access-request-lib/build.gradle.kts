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
  api("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.8.2")
  api("com.github.jknack:handlebars:4.5.2")
  api("com.github.spullara.mustache.java:compiler:0.9.14")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
}

publishing {
  repositories {
    mavenLocal()
  }
  publications {
    create<MavenPublication>("mainLibrary") {
      from(components["java"])
      pom {
        name.set(base.archivesName)
        artifactId = base.archivesName.get()
        description.set("A helper library to share common code related to subject access requests")
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
  sign(publishing.publications["mainLibrary"])
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

tasks.named("generateMetadataFileForMainLibraryPublication") {
  dependsOn("copyAgent")
}
