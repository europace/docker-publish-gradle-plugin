import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  `maven-publish`
  `java-gradle-plugin`
  kotlin("jvm") version "1.4.32"
  id("com.gradle.plugin-publish") version "0.10.1"
}

group = "de.europace.gradle"
version = now().format(ofPattern("yyyy-MM-dd\'T\'HH-mm-ss"))
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    "com.squareup.okio:okio:2.10.0"
)

val dependencyVersionsByGroup = mapOf<String, String>(
    "org.jetbrains.kotlin" to "1.4.32")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  implementation("de.gesellix:gradle-docker-plugin:2021-04-07T17-02-20")
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
  tags = listOf("docker", "publish", "publishing")
}

gradlePlugin {
  plugins {
    create("dockerPublishPlugin") {
      id = "de.europace.spring-boot.docker-publish"
      displayName = "Plugin do build and publish docker images of spring-boot services"
      description = "Adds a task to create and publish a docker image"
      implementationClass = "de.europace.spring.boot.docker.publish.DockerPublishPlugin"
    }
  }
}
