package de.europace.gradle.docker.publish

import de.gesellix.gradle.docker.DockerPlugin
import de.gesellix.gradle.docker.tasks.DockerBuildTask
import de.gesellix.gradle.docker.tasks.DockerPushTask
import de.gesellix.gradle.docker.tasks.DockerRmiTask
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME
import org.gradle.api.tasks.Copy

const val EXTENSION_NAME = "dockerPublish"

class DockerPublishPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val dockerBuildContextDir = File("${project.buildDir.path}/docker")
    val extension = project.extensions.findByName(EXTENSION_NAME) as? DockerPublishExtension ?: project.extensions.create(EXTENSION_NAME, DockerPublishExtension::class.java, project)

    project.pluginManager.apply(PublishingPlugin::class.java)
    project.pluginManager.apply(DockerPlugin::class.java)

    project.afterEvaluate {
      val prepareBuildContext = project.tasks.register("prepareBuildContext", Copy::class.java) {
        it.from(extension.dockerBuildContextSources)
        it.into(dockerBuildContextDir)
      }

      val artifactTask = project.tasks.getByName(extension.artifactTaskName.get())
      val copyArtifact = project.tasks.register("copyArtifact", Copy::class.java) {
        it.dependsOn(artifactTask)
        it.from(artifactTask)
        it.into(dockerBuildContextDir)
        it.rename { extension.artifactName.get() }
      }

      val buildImage = project.tasks.register("buildImage", DockerBuildTask::class.java) { buildTask ->
        buildTask.dependsOn(copyArtifact)
        buildTask.dependsOn(prepareBuildContext)
        buildTask.buildContextDirectory.set(dockerBuildContextDir)
        buildTask.imageName.set(dockerImageId(project, extension))
        buildTask.buildParams.set(mapOf("rm" to true, "pull" to true))
        buildTask.enableBuildLog.set(true)

        buildTask.doLast {
          project.logger.info("Image built as ${buildTask.imageName.get()}")
        }
      }

      val rmiLocalImage = project.tasks.register("rmiLocalImage", DockerRmiTask::class.java) {
        it.imageId.set(dockerImageId(project, extension))
      }

      val publishImage = project.tasks.register("publishImage", DockerPushTask::class.java) {
        it.dependsOn(buildImage)
        it.repositoryName.set(dockerImageId(project, extension))
        it.finalizedBy(rmiLocalImage)
        it.authConfig.set(project.provider { it.dockerClient.resolveAuthConfigForImage(dockerImageId(project, extension).get()) })
      }

      project.tasks.named(PUBLISH_LIFECYCLE_TASK_NAME) {
        it.finalizedBy(publishImage)
      }
    }
  }

  private fun dockerImageId(project: Project, extension: DockerPublishExtension) = project.providers.zip(
      getOrganisation(project, extension), getImageNameWithTag(project, extension)) { organisation, imageNameWithTag -> "$organisation/$imageNameWithTag" }

  private fun getOrganisation(project: Project, extension: DockerPublishExtension) = project.provider {
    if (!extension.organisation.isPresent) {
      throw GradleException("organisation must be set")
    }
    extension.organisation.get()
  }

  private fun getImageNameWithTag(project: Project, extension: DockerPublishExtension) = project.providers.zip(
      extension.imageName, extension.imageTag) { imageName, imageTag -> "$imageName:$imageTag" }
}
