package de.europace.gradle.docker.publish

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class DockerPublishExtension @Inject constructor(project: Project) {

  abstract val organisation: Property<String>
  abstract val imageName: Property<String>
  abstract val imageTag: Property<String>
  abstract val artifactTaskName: Property<String>
  abstract val artifactName: Property<String>
  abstract val dockerBuildContextSources: Property<String>

  init {
    artifactTaskName.convention("bootJar")
    artifactName.convention("application.jar")
    imageName.convention(project.name)
    imageTag.convention(project.version as String)
    dockerBuildContextSources.convention("${project.projectDir.path}/src/main/docker")
  }
}
