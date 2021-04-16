package de.europace.spring.boot.docker.publish

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property

@Suppress("UnnecessaryAbstractClass")
abstract class DockerPublishExtension @Inject constructor(project: Project) {

  abstract val organisation: Property<String>
  abstract val imageName: Property<String>
  abstract val imageTag: Property<String>
  abstract val dockerBuildContextSources: Property<String>
  abstract val dockerBuildContextDir: Property<String>

  init {
    imageName.convention(project.name)
    imageTag.convention(project.version as String)
    dockerBuildContextSources.convention("${project.projectDir.path}/src/main/docker")
    dockerBuildContextDir.convention("${project.buildDir.path}/docker")
  }
}
