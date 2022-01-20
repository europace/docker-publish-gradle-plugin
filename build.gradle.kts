buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
val javaVersion = JavaVersion.VERSION_1_8
val kotestVersion = "4.6.4"
val kotlinVersion = "1.6.10" // remember to update in plugins
val mockkVersion = "1.12.2"

plugins {
  `maven-publish`
  `java-gradle-plugin`
  kotlin("jvm") version "1.6.10" // remember to update in dependencyVersionsByGroup
  id("com.gradle.plugin-publish") version "0.20.0"
}

group = "de.europace.gradle"
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    "com.squareup.okio:okio:3.0.0",
    "io.mockk:mockk:$mockkVersion"
)

val dependencyVersionsByGroup = mapOf(
    "org.jetbrains.kotlin" to kotlinVersion
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
  implementation("de.gesellix:gradle-docker-plugin:2021-12-18T23-58-00")

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
  tags = listOf("docker", "publish", "publishing")
}

gradlePlugin {
  plugins {
    create("dockerPublishPlugin") {
      id = "de.europace.docker-publish"
      displayName = "Docker Publish Plugin"
      description = "Adds tasks to create and publish a Docker image to a registry"
      implementationClass = "de.europace.gradle.docker.publish.DockerPublishPlugin"
    }
  }
}
