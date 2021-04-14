# Docker Publish Gradle Plugin

A Gradle plugin to build and publish a spring-boot-service to docker hub

## Usage

The plugin is available on the official [Gradle plugin portal](https://plugins.gradle.org/plugin/de.europace.gradle.docker-publish). You need to apply it to the subprojects you want to publish to
docker hub.

    plugins {
      id("de.europace.gradle.docker-publish") version "..."
    }

## Contributing

Please submit issues if you have any questions or suggestions regarding this plugin. Code changes like bug fixes or new features can be proposed as pull requests.

## Publishing

Publishing requires username and API key from the project owner. Please see [the docs](https://plugins.gradle.org/docs/submit) for details.
