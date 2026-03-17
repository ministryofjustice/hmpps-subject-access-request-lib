plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.5"
  kotlin("plugin.spring") version "2.3.10"
  kotlin("jvm") version "2.3.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.2")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.10")
  runtimeOnly("com.h2database:h2:2.4.240")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.2")
  testImplementation(project(":hmpps-subject-access-request-test-support"))
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

tasks.named<Test>("test") {
  dependsOn(
    project(":hmpps-subject-access-request-lib").tasks.named("copyAgent"),
    project(":hmpps-subject-access-request-test-support").tasks.named("copyAgent"),
  )
}
