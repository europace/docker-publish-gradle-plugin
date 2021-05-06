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

    fun dockerImageId() = "${getOrganisation(extension)}/${extension.imageName.get()}:${extension.imageTag.get()}"

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

      val buildImage = project.tasks.register("buildImage", DockerBuildTask::class.java) {
        it.dependsOn(copyArtifact)
        it.dependsOn(prepareBuildContext)
        it.buildContextDirectory.set(dockerBuildContextDir)
        it.imageName.set(dockerImageId())
        it.buildParams.set(mapOf("rm" to true, "pull" to true))
        it.enableBuildLog.set(true)

        it.doLast {
          project.logger.info("Image built as ${dockerImageId()}")
        }
      }

      val rmiLocalImage = project.tasks.register("rmiLocalImage", DockerRmiTask::class.java) {
        it.imageId.set(dockerImageId())
      }

      val publishImage = project.tasks.register("publishImage", DockerPushTask::class.java) {
        it.dependsOn(buildImage)
        it.repositoryName.set(dockerImageId())
        it.finalizedBy(rmiLocalImage)
      }

      project.tasks.named(PUBLISH_LIFECYCLE_TASK_NAME) {
        it.finalizedBy(publishImage)
      }
    }
  }

  private fun getOrganisation(extension: DockerPublishExtension): String {
    return extension.organisation.getOrNull() ?: throw GradleException("organisation must be set")
  }
}
