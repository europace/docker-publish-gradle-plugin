package de.europace.spring.boot.docker.publish

import org.gradle.api.provider.Property

interface DockerPublishExtension {

  val organisation: Property<String>
  val imageName: Property<String?>
  val imageTag: Property<String?>
  val dockerBuildContextSources: Property<String?>
  val dockerBuildContextDir: Property<String?>
}
