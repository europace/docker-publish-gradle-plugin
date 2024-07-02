val javaVersion = JavaVersion.VERSION_17

plugins {
  alias(libs.plugins.kotlinJvm)
  id("com.gradle.plugin-publish") version "1.2.1"
}

group = "de.europace.gradle"
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    libs.annotations,
    libs.mockk,
    libs.okio,
    libs.opentest4j
)

val dependencyVersionsByGroup = mapOf(
    "net.bytebuddy" to libs.versions.byteBuddy.get(),
    "org.jetbrains.kotlin" to libs.versions.kotlin.get(),
    "org.jetbrains.kotlinx" to libs.versions.kotlinx.get()
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
  implementation(libs.gradleDocker)

  testImplementation(libs.bundles.kotest)
  testImplementation(libs.mockk)
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
  publishing {
    publications {
      register("pluginMaven", MavenPublication::class) {
        pom {
          url.set("https://github.com/europace/docker-publish-gradle-plugin")
        }
      }
    }
  }
}
