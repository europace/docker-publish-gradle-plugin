package de.europace.gradle.docker.publish

import de.gesellix.gradle.docker.tasks.DockerBuildTask
import de.gesellix.gradle.docker.tasks.DockerPushTask
import de.gesellix.gradle.docker.tasks.DockerRmiTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME
import org.gradle.api.tasks.Copy

open class DockerPublishPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension: DockerPublishExtension = project.extensions.create("dockerPublish", DockerPublishExtension::class.java)

    project.afterEvaluate {
      val orga = extension.orga.get()
      val imageName = extension.imageName.getOrElse(project.name)
      val dockerBuildContextSources = extension.dockerBuildContextSources.getOrElse("${project.projectDir.path}/src/main/docker")
      val dockerBuildContextDir = extension.dockerBuildContextDir.getOrElse("${project.buildDir.path}/docker")
      val imageTag = extension.dockerImageTag.get()

      val dockerImageName = "$orga/$imageName"
      val dockerImageId = "$dockerImageName:${imageTag ?: project.version}"

      project.pluginManager.apply(PublishingPlugin::class.java)

      val prepareBuildContext = project.tasks.register("prepareBuildContext", Copy::class.java) {
        it.from(dockerBuildContextSources)
        it.into(dockerBuildContextDir)
      }

      val copyArtifact = project.tasks.register("copyArtifact", Copy::class.java) {
        it.dependsOn(project.tasks.getByName("bootJar"))
        it.dependsOn(prepareBuildContext)
        it.from(project.tasks.getByName(("bootJar")))
        it.into(dockerBuildContextDir)
        it.rename { "application.jar" }
      }

      val buildImage = project.tasks.register("buildImage", DockerBuildTask::class.java) {
        it.dependsOn(copyArtifact)
        it.setBuildContextDirectory(dockerBuildContextDir)
        it.imageName = dockerImageId
        it.buildParams = mapOf("rm" to true, "pull" to true)
        it.enableBuildLog = true

        it.doLast {
          project.logger.info("Image built as $dockerImageId")
        }
      }

      val rmiLocalImage = project.tasks.register("rmiLocalImage", DockerRmiTask::class.java) {
        it.imageId = dockerImageId
      }

      val publishImage = project.tasks.register("publishImage", DockerPushTask::class.java) {
        it.dependsOn(buildImage)
        it.repositoryName = dockerImageId
        it.finalizedBy(rmiLocalImage)
      }

      project.tasks.named(PUBLISH_LIFECYCLE_TASK_NAME) {
        it.finalizedBy(publishImage)
      }
    }
  }
}
