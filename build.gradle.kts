import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaVersion = JavaVersion.VERSION_17
val jvmVersion = JvmTarget.JVM_17

plugins {
  alias(libs.plugins.kotlinJvm)
  id("com.gradle.plugin-publish") version "1.3.1"
}

group = "de.europace.gradle"
logger.lifecycle("version: $version")

val dependencyVersions = listOf(
    libs.annotations,
    libs.junitBom,
    libs.mockk,
    libs.okio,
    libs.okioJvm,
    libs.opentest4j,
    libs.slf4j,
    libs.kotlinxBom,
    libs.kotlinxIo
)

val dependencyVersionsByGroup = mapOf(
    "net.bytebuddy" to libs.versions.byteBuddy.get(),
    "net.java.dev.jna" to libs.netJavaDev.get().version,
    "org.jetbrains.kotlin" to libs.versions.kotlin.get(),
    "org.junit.jupiter" to libs.versions.junitJupiter.get(),
    "org.junit.platform" to libs.versions.junitPlatform.get(),
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
    compilerOptions {
      jvmTarget.set(jvmVersion)
      freeCompilerArgs.set(listOf("-Xjsr305=strict"))
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

val scmUrl = "github.com/europace/docker-publish-gradle-plugin"
gradlePlugin {
  website.set("https://$scmUrl")
  vcsUrl.set("https://$scmUrl")
  plugins {
    register("dockerPublishPlugin") {
      id = "de.europace.docker-publish"
      displayName = "Docker Publish Plugin"
      description = "Adds tasks to create and publish a Docker image to a registry"
      implementationClass = "de.europace.gradle.docker.publish.DockerPublishPlugin"
      tags.set(listOf("docker", "publish", "publishing"))
    }
  }
}
publishing {
  publications {
    register<MavenPublication>("dockerPublishPlugin") {
      from(components["java"])
      pom {
        url.set("https://$scmUrl")
        scm {
          connection.set("scm:git:$scmUrl")
          developerConnection.set("scm:git:ssh://$scmUrl")
          url.set("https://$scmUrl")
        }
      }
    }
  }
}
