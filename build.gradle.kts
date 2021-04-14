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
  id("de.gesellix.docker") version "2021-04-07T17-02-20"
  id("com.gradle.plugin-publish") version "0.10.1"
}

group = "de.europace.gradle"
version = now().format(ofPattern("yyyy-MM-dd\'T\'HH-mm-ss"))
logger.lifecycle("version: $version")

val dependencyVersions = listOf<String>(
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
      id = "de.europace.gradle.docker-publish"
      displayName = "Plugin do create and publish docker images"
      description = "Adds a task to publish a created docker image"
      implementationClass = "de.europace.gradle.docker.publish.DockerPublishPlugin"
    }
  }
}

publishing {
  repositories {
    if (project.hasProperty("maven.publish.url") && project.hasProperty("maven.publish.username") && project.hasProperty("maven.publish.password")) {
      maven {
        name = "Maven"
        setUrl(project.property("maven.publish.url") as String)
        credentials.apply {
          username = project.property("maven.publish.username") as String
          password = project.property("maven.publish.password") as String
        }
      }
    }
  }
}
