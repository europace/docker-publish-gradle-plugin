val javaVersion = JavaVersion.VERSION_1_8
val junitVersion = "5.9.1"
val kotestVersion = "5.6.2"
val kotlinVersion = "1.8.22" // remember to update in plugins
val kotlinxVersion = "1.6.4"
val mockkVersion = "1.13.7"

plugins {
  kotlin("jvm") version "1.9.0" // remember to update in dependency
  id("com.gradle.plugin-publish") version "1.2.0"
}

group = "de.europace.gradle"
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    "com.squareup.okio:okio:3.5.0",
    "io.mockk:mockk:$mockkVersion"
)

val dependencyVersionsByGroup = mapOf(
    "net.bytebuddy" to "1.12.10",
    "org.jetbrains.kotlin" to kotlinVersion,
    "org.jetbrains.kotlinx" to kotlinxVersion,
    "org.junit" to junitVersion,
    "org.junit.jupiter" to junitVersion,
    "org.junit.platform" to "1.9.1",
    "org.slf4j" to "1.7.36"
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
  implementation("de.gesellix:gradle-docker-plugin:2023-08-16T09-45-00")

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

gradlePlugin {
  website.set("https://github.com/europace/docker-publish-gradle-plugin")
  vcsUrl.set("https://github.com/europace/docker-publish-gradle-plugin")
  plugins {
    create("dockerPublishPlugin") {
      id = "de.europace.docker-publish"
      displayName = "Docker Publish Plugin"
      description = "Adds tasks to create and publish a Docker image to a registry"
      implementationClass = "de.europace.gradle.docker.publish.DockerPublishPlugin"
      tags.set(listOf("docker", "publish", "publishing"))
    }
  }
}
