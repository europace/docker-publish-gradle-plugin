package de.europace.gradle.docker.publish

import org.gradle.api.provider.Property

interface DockerPublishExtension {

  val orga: Property<String>
  val imageName: Property<String?>
  val dockerBuildContextSources: Property<String?>
  val dockerBuildContextDir: Property<String?>
  val dockerImageTag: Property<String?>
}
