import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
val javaVersion = JavaVersion.VERSION_1_8
val kotestVersion = "4.4.3"
val mockkVersion = "1.11.0"

plugins {
  `maven-publish`
  `java-gradle-plugin`
  kotlin("jvm") version "1.4.32"
  id("com.gradle.plugin-publish") version "0.14.0"
}

group = "de.europace.gradle"
version = now().format(ofPattern("yyyy-MM-dd\'T\'HH-mm-ss"))
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    "com.squareup.okio:okio:2.10.0",
    "io.mockk:mockk:$mockkVersion",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3"
)

val dependencyVersionsByGroup = mapOf<String, String>(
  "org.jetbrains.kotlin" to "1.4.32"
)

java {
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}

tasks {
  withType<Test> {
    useJUnitPlatform()
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = javaVersion.toString()
      freeCompilerArgs = listOf("-Xjsr305=strict")
    }
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  implementation("de.gesellix:gradle-docker-plugin:2021-04-07T17-02-20")

  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testImplementation("io.kotest:kotest-framework-engine-jvm:$kotestVersion")
  testImplementation("io.kotest:kotest-property-jvm:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
  testImplementation("io.mockk:mockk:$mockkVersion")
}

allprojects {
  configurations.all {
    resolutionStrategy {
      failOnVersionConflict()
      force(dependencyVersions)
      eachDependency {
        val forcedVersion = dependencyVersionsByGroup[requested.group]
        if (forcedVersion != null) {
          useVersion(forcedVersion)
        }
      }
      cacheDynamicVersionsFor(0, "seconds")
    }
  }
}

pluginBundle {
  website = "https://github.com/europace/docker-publish-gradle-plugin"
  vcsUrl = "https://github.com/europace/docker-publish-gradle-plugin"
  tags = listOf("docker", "spring-boot", "publish", "publishing")
}

gradlePlugin {
  plugins {
    create("dockerPublishPlugin") {
      id = "de.europace.spring-boot.docker-publish"
      displayName = "Plugin to build and publish docker images of spring-boot services"
      description = "Adds tasks to create and publish a docker image from a sping-boot jar file"
      implementationClass = "de.europace.spring.boot.docker.publish.DockerPublishPlugin"
    }
  }
}
