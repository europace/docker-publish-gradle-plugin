package de.europace.gradle.docker.publish

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure

const val PLUGIN_ID = "de.europace.docker-publish"

class DockerPublishPluginIntegrationTest : FreeSpec() {

  private val testProjectDir = tempdir()

  private lateinit var buildFile: File

  init {

    beforeTest {
      buildFile = File(testProjectDir, "build.gradle.kts")
    }

    "publishImage should have tasks in right order" {
      buildFile.writeText(
          """
          plugins {
            id("$PLUGIN_ID")
          }
          
          dockerPublish {
            organisation.set("foo")
          }
          
          tasks {
            create("bootJar") {
              doFirst {
                logger.lifecycle("Would now create jar file")
              }
            }
          }
          """.trimIndent()
      )

      val result = GradleRunner.create()
          .withProjectDir(testProjectDir)
          .withPluginClasspath()
          .withArguments("publishImage", "--dry-run")
          .forwardOutput()
          .build()

      val expectedOutput = """
        :bootJar SKIPPED
        :copyArtifact SKIPPED
        :prepareBuildContext SKIPPED
        :buildImage SKIPPED
        :publishImage SKIPPED
        :rmiLocalImage SKIPPED
        """.trimIndent()
      result.output.normalizedLineSeparators() shouldStartWith expectedOutput.normalizedLineSeparators()
      result.output shouldContain "BUILD SUCCESSFUL"
    }

    "publishImage should lookup the authConfig without errors" {
      buildFile.writeText(
          """
          plugins {
            id("$PLUGIN_ID")
          }

          dockerPublish {
            organisation.set("foo")
          }

          tasks {
            create("bootJar") {
              doFirst {
                logger.lifecycle("Would now create jar file")
              }
            }
            withType<de.gesellix.gradle.docker.tasks.DockerPushTask>().matching { it.name == "publishImage" }.configureEach {
              doFirst {
                logger.lifecycle("image: ${"$"}{repositoryName.get()}, authConfig: ${"$"}{if (authConfig.get() == null) {"missing"} else {"found"}}")
                throw GradleException("won't continue with ${"$"}{this.name}")
              }
            }
          }
          """.trimIndent()
      )

      val exception = shouldThrow<UnexpectedBuildFailure> {
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("publishImage", "-x", "buildImage", "--info", "--stacktrace")
            .forwardOutput()
            .build()
      }

      exception.message shouldContain "authConfig: found"
      exception.message shouldContain "won't continue with publishImage"
    }

    "publishImage should fail if no organisation is set" {
      buildFile.writeText(
          """
          plugins {
              id("$PLUGIN_ID")
          }
          
          tasks {
            create("bootJar") {
              doFirst{
                   logger.lifecycle("Would now create jar file")
              }
            }
          }
          """.trimIndent()
      )

      val exception = shouldThrow<UnexpectedBuildFailure> {

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("publishImage")
            .forwardOutput()
            .build()
      }
      val expectedOutput = """
        Execution failed for task ':buildImage'.
        > Failed to calculate the value of task ':buildImage' property 'imageName'.
           > organisation must be set
        """.trimIndent()

      exception.message?.normalizedLineSeparators() shouldContain expectedOutput.normalizedLineSeparators()
      exception.message shouldContain "BUILD FAILED"
    }

    "publishImage should fail if no bootJar task is available" {
      buildFile.writeText(
          """
          plugins {
              id("$PLUGIN_ID")
          }
          
          dockerPublish {
            organisation.set("foo")
          }
          """.trimIndent()
      )

      val exception = shouldThrow<UnexpectedBuildFailure> {

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("publishImage", "--dry-run")
            .forwardOutput()
            .build()
      }
      val expectedOutput = "> Task with name 'bootJar' not found in root project"

      exception.message shouldContain expectedOutput
      exception.message shouldContain "BUILD FAILED"
    }
  }
}

fun String.normalizedLineSeparators(): String = this.replace("[\n\r]+".toRegex(), "\n")
