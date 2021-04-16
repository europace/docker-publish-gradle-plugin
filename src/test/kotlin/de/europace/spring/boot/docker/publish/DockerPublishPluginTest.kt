package de.europace.spring.boot.docker.publish

import de.gesellix.gradle.docker.DockerPlugin
import de.gesellix.gradle.docker.tasks.DockerBuildTask
import de.gesellix.gradle.docker.tasks.DockerPushTask
import de.gesellix.gradle.docker.tasks.DockerRmiTask
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder

class DockerPublishPluginTest : FreeSpec() {

  init {

    "should apply the plugin by using the ID" {
      val project = ProjectBuilder.builder().build()
      project.createDockerPublishExtension()

      project.pluginManager.apply("de.europace.spring-boot.docker-publish")

      project.plugins.getPlugin(DockerPublishPlugin::class.java) shouldNotBe null
    }

    "should apply DockerPlugin" {
      val project = ProjectBuilder.builder().build()
      project.createDockerPublishExtension()

      project.pluginManager.apply(DockerPublishPlugin::class.java)

      project.plugins.getPlugin(DockerPlugin::class.java) shouldNotBe null
    }

    "should apply PublishPlugin" {
      val project = ProjectBuilder.builder().build()
      project.createDockerPublishExtension()

      project.pluginManager.apply(DockerPublishPlugin::class.java)

      project.plugins.getPlugin(PublishingPlugin::class.java) shouldNotBe null

      val task = (project.tasks.getByName(PUBLISH_LIFECYCLE_TASK_NAME) as DefaultTask)
      task.finalizedByElement().name shouldBe "publishImage"

    }

    "prepareBuildContext" - {
      "should use default destinationDir" {
        val project = ProjectBuilder.builder().build()
        project.createDockerPublishExtension()

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        (project.tasks.getByName("prepareBuildContext") as Copy).destinationDir.path shouldEndWith "/docker"
      }

      "should use defined destinationDir" {
        val expectedDir = "someDir"
        val project = ProjectBuilder.builder().build()
        val extension = project.createDockerPublishExtension()
        extension.dockerBuildContextDir.value(expectedDir)

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        (project.tasks.getByName("prepareBuildContext") as Copy).destinationDir.path shouldEndWith expectedDir
      }
    }

    "copyArtifact" - {
      "should use default values" {
        val project = ProjectBuilder.builder().build()
        project.createDockerPublishExtension()
        project.tasks.register("bootJar", DefaultTask::class.java)


        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = project.tasks.getByName("copyArtifact") as Copy
        task.dependsOn.any { (it as? TaskProvider<*>)?.name == "prepareBuildContext" } shouldBe true
        task.destinationDir.path shouldEndWith "/docker"
      }

      "should use defined destinationDir" {
        val expectedDir = "someDir"
        val project = ProjectBuilder.builder().build()
        val extension = project.createDockerPublishExtension()
        extension.dockerBuildContextDir.value(expectedDir)

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        (project.tasks.getByName("prepareBuildContext") as Copy).destinationDir.path shouldEndWith expectedDir
      }
    }

    "buildImage" - {
      "should set correct default values"{
        val project = ProjectBuilder.builder().build()
        project.createDockerPublishExtension("expectedOrganisation")

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = (project.tasks.getByName("buildImage") as DockerBuildTask)
        task.dependsOn.any { (it as? TaskProvider<*>)?.name == "copyArtifact" } shouldBe true
        task.buildContextDirectory.path shouldEndWith "/docker"
        task.imageName shouldBe "expectedOrganisation/${project.name}:${project.version}"
        task.buildParams shouldBe mapOf("rm" to true, "pull" to true)
        task.enableBuildLog shouldBe true
      }

      "should set correct defined values"{
        val expectedDir = "expectedDir"
        val expectedName = "expectedName"
        val expectedTag = "expectedTag"
        val project = ProjectBuilder.builder().build()
        val extension = project.createDockerPublishExtension("expectedOrganisation")
        extension.dockerBuildContextDir.value(expectedDir)
        extension.imageTag.value(expectedTag)
        extension.imageName.value(expectedName)

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = (project.tasks.getByName("buildImage") as DockerBuildTask)
        task.buildContextDirectory.path shouldEndWith expectedDir
        task.imageName shouldBe "expectedOrganisation/$expectedName:$expectedTag"
      }
    }

    "rmiLocalImage" - {
      "should set correct default values"{
        val project = ProjectBuilder.builder().build()
        project.createDockerPublishExtension("expectedOrganisation")

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = (project.tasks.getByName("rmiLocalImage") as DockerRmiTask)
        task.imageId shouldBe "expectedOrganisation/${project.name}:${project.version}"
      }

      "should set correct defined values"{
        val expectedDir = "expectedDir"
        val expectedName = "expectedName"
        val expectedTag = "expectedTag"
        val project = ProjectBuilder.builder().build()
        val extension = project.createDockerPublishExtension("expectedOrganisation")
        extension.dockerBuildContextDir.value(expectedDir)
        extension.imageTag.value(expectedTag)
        extension.imageName.value(expectedName)

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = (project.tasks.getByName("rmiLocalImage") as DockerRmiTask)
        task.imageId shouldBe "expectedOrganisation/$expectedName:$expectedTag"
      }
    }

    "publishImage" - {
      "should set correct default values"{
        val project = ProjectBuilder.builder().build()
        project.createDockerPublishExtension("expectedOrganisation")

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = (project.tasks.getByName("publishImage") as DockerPushTask)
        task.dependsOn.any { (it as? TaskProvider<*>)?.name == "buildImage" } shouldBe true
        task.finalizedByElement().name shouldBe "rmiLocalImage"
        task.repositoryName shouldBe "expectedOrganisation/${project.name}:${project.version}"
      }

      "should set correct defined values"{
        val expectedDir = "expectedDir"
        val expectedName = "expectedName"
        val expectedTag = "expectedTag"
        val project = ProjectBuilder.builder().build()
        val extension = project.createDockerPublishExtension("expectedOrganisation")
        extension.dockerBuildContextDir.value(expectedDir)
        extension.imageTag.value(expectedTag)
        extension.imageName.value(expectedName)

        project.pluginManager.apply(DockerPublishPlugin::class.java)

        val task = (project.tasks.getByName("publishImage") as DockerPushTask)
        task.repositoryName shouldBe "expectedOrganisation/$expectedName:$expectedTag"
      }
    }
  }

  fun Project.createDockerPublishExtension(organisation: String = "someOrganisation"): DockerPublishExtension {

    val extension = project.extensions.create("dockerPublish", DockerPublishExtension::class.java, this)
    extension.organisation.value(organisation)
    return extension
  }

  fun DefaultTask.finalizedByElement() = (this.finalizedBy as DefaultTaskDependency).mutableValues.elementAt(0) as TaskProvider<*>
}
