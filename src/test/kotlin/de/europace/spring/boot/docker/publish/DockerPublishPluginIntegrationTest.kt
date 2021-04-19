package de.europace.spring.boot.docker.publish

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure

const val PLUGIN_ID = "de.europace.spring-boot.docker-publish"

class DockerPublishPluginIntegrationTest : FreeSpec() {

  @Rule
  private val testProjectDir = TemporaryFolder()
  private lateinit var buildFile: File

  init {

    beforeSpec {
      testProjectDir.create()
    }

    beforeTest {
      buildFile = testProjectDir.newFile("build.gradle.kts")
    }

    "buildImage should have tasks in right order" {
      val project = ProjectBuilder.builder().build()
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
            doFirst{
                 logger.lifecycle("Would now create jar file")
            }
          }
        }
    """
      )

      val result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("publishImage", "--dry-run")
        .forwardOutput()
        .build()

      val expectedOutput = """:bootJar SKIPPED
:prepareBuildContext SKIPPED
:copyArtifact SKIPPED
:buildImage SKIPPED
:publishImage SKIPPED
:rmiLocalImage SKIPPED
"""
      result.output shouldStartWith expectedOutput
      result.output shouldContain "BUILD SUCCESSFUL"
    }

    "buildImage fail if no organisation is set " {
      val project = ProjectBuilder.builder().build()
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
    """
      )

      val exception = shouldThrow<UnexpectedBuildFailure> {

        GradleRunner.create()
          .withProjectDir(testProjectDir.root)
          .withPluginClasspath()
          .withArguments("publishImage", "--dry-run")
          .forwardOutput()
          .build()

      }
      val expectedOutput = """> Could not create task ':publishImage'.
   > organisation must be set"""

      exception.message shouldContain expectedOutput
      exception.message shouldContain "BUILD FAILED"
    }

    "buildImage fail if no bootJar task is available " {
      val project = ProjectBuilder.builder().build()
      buildFile.writeText(
        """
        plugins {
            id("$PLUGIN_ID")
        }
        
        dockerPublish {
          organisation.set("foo")
        }"""
      )

      val exception = shouldThrow<UnexpectedBuildFailure> {

        GradleRunner.create()
          .withProjectDir(testProjectDir.root)
          .withPluginClasspath()
          .withArguments("publishImage", "--dry-run")
          .forwardOutput()
          .build()

      }
      val expectedOutput = """Could not determine the dependencies of task ':buildImage'.
> Could not create task ':copyArtifact'.
   > Task with name 'bootJar' not found in root project """

      exception.message shouldContain expectedOutput
      exception.message shouldContain "BUILD FAILED"
    }
  }
}
