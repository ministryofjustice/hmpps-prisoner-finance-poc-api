import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.2"
  kotlin("plugin.spring") version "2.2.20"
  id("org.jetbrains.kotlin.plugin.noarg") version "2.2.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.7.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  runtimeOnly("org.postgresql:postgresql:42.7.8")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.7.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.34") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.h2database:h2")
}

kotlin {
  jvmToolchain(21)
  noArg {
    annotation("jakarta.persistence.Entity")
  }
}

tasks {
  register<Test>("unitTest") {
    filter {
      excludeTestsMatching("uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration*")
    }
  }

  register<Test>("integrationTest") {
    description = "Runs the integration tests, make sure that dependencies are available first by running `make serve`."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["main"].output + configurations["testRuntimeClasspath"] + sourceSets["test"].output
    filter {
      includeTestsMatching("uk.gov.justice.digital.hmpps.prisonerfinancepocapi.integration*")
    }
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
    }
  }

  testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
  }
}
