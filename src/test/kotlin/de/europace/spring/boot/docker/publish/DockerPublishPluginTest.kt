package de.europace.spring.boot.docker.publish

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder

class DockerPublishPluginTest : FreeSpec() {

  init {

    "Using the Plugin ID should apply the Plugin" {
      val project = ProjectBuilder.builder().build()
      project.pluginManager.apply("de.europace.spring-boot.docker-publish")

      project.plugins.getPlugin(DockerPublishPlugin::class.java) shouldNotBe null
    }
  }
}
