package de.europace.gradle.docker.publish

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
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder

class DockerPublishPluginTest : FreeSpec() {

  init {

    "should apply the plugin by using the ID" {
      val project = createProject()
      project.createDockerPublishExtension()

      project.pluginManager.apply("de.europace.docker-publish")

      project.plugins.getPlugin(DockerPublishPlugin::class.java) shouldNotBe null
    }

    "should apply DockerPlugin" {
      val project = createProject()
      project.createDockerPublishExtension()

      project.pluginManager.apply(DockerPublishPlugin::class.java)

      project.plugins.getPlugin(DockerPlugin::class.java) shouldNotBe null
    }

    "should apply PublishPlugin" {
      val project = createProject().withArtifactTask()
      project.createDockerPublishExtension()

      project.pluginManager.apply(DockerPublishPlugin::class.java)
      project.evaluate()

      project.plugins.getPlugin(PublishingPlugin::class.java) shouldNotBe null

      val task = (project.tasks.getByName(PUBLISH_LIFECYCLE_TASK_NAME) as DefaultTask)
      task.finalizedByElement().name shouldBe "publishImage"
    }

    "prepareBuildContext" - {
      "should use default destinationDir" {
        val project = createProject().withArtifactTask()
        project.createDockerPublishExtension()

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        (project.tasks.getByName("prepareBuildContext") as Copy).destinationDir.path shouldEndWith "/docker"
      }
    }

    "copyArtifact" - {
      "should use default values" {
        val project = createProject().withArtifactTask()
        project.createDockerPublishExtension()

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = project.tasks.getByName("copyArtifact") as Copy
        task.dependsOn.any { (it as? DefaultTask)?.name == "bootJar" } shouldBe true
        task.destinationDir.path shouldEndWith "/docker"
      }

      "should use defined artifactTask" {
        val expectedName = "someName"
        val project = createProject()
        val extension = project.createDockerPublishExtension()
        extension.artifactTaskName.value(expectedName)
        project.tasks.register(expectedName, DefaultTask::class.java)

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = project.tasks.getByName("copyArtifact") as Copy
        task.dependsOn.any { (it as? DefaultTask)?.name == expectedName } shouldBe true
      }
    }

    "buildImage" - {
      "should set correct default values" {
        val project = createProject().withArtifactTask()
        project.createDockerPublishExtension()

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = (project.tasks.getByName("buildImage") as DockerBuildTask)
        task.dependsOn.any { (it as? TaskProvider<*>)?.name == "copyArtifact" } shouldBe true
        task.dependsOn.any { (it as? TaskProvider<*>)?.name == "prepareBuildContext" } shouldBe true
        task.buildContextDirectory.asFile.get().path shouldEndWith "/docker"
        task.imageName.get() shouldBe "some-organisation/${project.name}:${project.version}"
        task.buildParams.get() shouldBe mapOf("rm" to true, "pull" to true)
        task.enableBuildLog.get() shouldBe true
      }

      "should evaluate the project version lazily" {
        val project = createProject().withArtifactTask()
        project.createDockerPublishExtension()

        project.version = "version-before-evaluate"
        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()
        project.version = "version-after-evaluate"

        val task = (project.tasks.getByName("buildImage") as DockerBuildTask)
        task.imageName.get() shouldBe "some-organisation/${project.name}:version-after-evaluate"
      }

      "should set correct defined values" {
        val expectedName = "expectedName"
        val expectedTag = "expectedTag"
        val project = createProject().withArtifactTask()
        val extension = project.createDockerPublishExtension("expectedOrganisation")
        extension.imageTag.set(expectedTag)
        extension.imageName.set(expectedName)

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = (project.tasks.getByName("buildImage") as DockerBuildTask)
        task.imageName.get() shouldBe "expectedOrganisation/$expectedName:$expectedTag"
      }
    }

    "rmiLocalImage" - {
      "should set correct default values" {
        val project = createProject().withArtifactTask()
        project.createDockerPublishExtension()

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = (project.tasks.getByName("rmiLocalImage") as DockerRmiTask)
        task.imageId.get() shouldBe "some-organisation/${project.name}:${project.version}"
      }

      "should set correct defined values" {
        val expectedName = "expectedName"
        val expectedTag = "expectedTag"
        val project = createProject().withArtifactTask()
        val extension = project.createDockerPublishExtension("expectedOrganisation")
        extension.imageTag.value(expectedTag)
        extension.imageName.value(expectedName)

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = (project.tasks.getByName("rmiLocalImage") as DockerRmiTask)
        task.imageId.get() shouldBe "expectedOrganisation/$expectedName:$expectedTag"
      }
    }

    "publishImage" - {
      "should set correct default values" {
        val project = createProject().withArtifactTask()
        project.createDockerPublishExtension()

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = (project.tasks.getByName("publishImage") as DockerPushTask)
        task.dependsOn.any { (it as? TaskProvider<*>)?.name == "buildImage" } shouldBe true
        task.finalizedByElement().name shouldBe "rmiLocalImage"
        task.repositoryName.get() shouldBe "some-organisation/${project.name}:${project.version}"
        task.authConfig.get() shouldNotBe null
      }

      "should set correct defined values" {
        val expectedName = "expectedName"
        val expectedTag = "expectedTag"
        val project = createProject().withArtifactTask()
        val extension = project.createDockerPublishExtension("expectedOrganisation")
        extension.imageTag.set(expectedTag)
        extension.imageName.set(expectedName)

        project.pluginManager.apply(DockerPublishPlugin::class.java)
        project.evaluate()

        val task = (project.tasks.getByName("publishImage") as DockerPushTask)
        task.repositoryName.get() shouldBe "expectedOrganisation/$expectedName:$expectedTag"
      }
    }
  }

  private fun createProject() = ProjectBuilder.builder().build() as ProjectInternal

  private fun ProjectInternal.withArtifactTask(): ProjectInternal {
    this.project.tasks.register("bootJar", DefaultTask::class.java)
    return this
  }

  private fun Project.createDockerPublishExtension(organisation: String = "some-organisation"): DockerPublishExtension =
      project.extensions.create("dockerPublish", DockerPublishExtension::class.java, this).apply {
        this.organisation.set(organisation)
      }

  private fun DefaultTask.finalizedByElement() = (this.finalizedBy as DefaultTaskDependency).mutableValues.elementAt(0) as TaskProvider<*>
}
