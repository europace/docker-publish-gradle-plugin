buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
val javaVersion = JavaVersion.VERSION_1_8
val kotestVersion = "4.6.0"
val mockkVersion = "1.12.0"

plugins {
  `maven-publish`
  `java-gradle-plugin`
  kotlin("jvm") version "1.5.20" // remember to update in dependencyVersionsByGroup
  id("com.gradle.plugin-publish") version "0.15.0"
}

group = "de.europace.gradle"
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    "com.squareup.okio:okio:2.10.0",
    "io.mockk:mockk:$mockkVersion",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.1"
)

val dependencyVersionsByGroup = mapOf(
    "org.jetbrains.kotlin" to "1.5.20" // remember to update in plugin section as well
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
  implementation("de.gesellix:gradle-docker-plugin:2021-05-05T22-41-13")

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
      description = "Adds tasks to create and publish a docker image to docker hub"
      implementationClass = "de.europace.gradle.docker.publish.DockerPublishPlugin"
    }
  }
}
