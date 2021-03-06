# Docker Publish Gradle Plugin

A Gradle plugin to build and publish a docker image to docker hub. The plugin was created to centralise code in our spring-boot microservices. Therefore some of the default values are related to
the [spring-boot-plugin](https://plugins.gradle.org/plugin/org.springframework.boot).

## Usage

The plugin is available on the official [Gradle plugin portal](https://plugins.gradle.org/plugin/de.europace.docker-publish). You need to apply it to the subprojects you want to publish to docker hub.

    plugins {
      id("de.europace.docker-publish") version "..."
    }

    dockerPublish{
        organisation.set("my-dockerhub-organisation")
    }

## Defaults

* The `publishImage` task is configured to run after Gradle's `publish` task, so this plugin implicitly applies the `maven-publish` plugin.
* Authentication to the default registry (Docker Hub) is enabled by default.

### Configuration

The configuration of the plugin is done in a block called `dockerPublish`

| Name                      | Description                                                | Default Value                                |
|---------------------------|------------------------------------------------------------|----------------------------------------------|
| organisation              | the namespace/organisation of the target docker repository | mandatory                                    |
| imageName                 | the name of your docker repository                         | project.name                                 |
| imageTag                  | the tag of your docker image                               | project.version                              |
| artifactTaskName          | the name of the task the docker image depends on           | "bootJar"                                    |
| dockerBuildContextSources | The location (folder) of your docker file                  | "${project.projectDir.path}/src/main/docker" |

## Contributing

Please submit issues if you have any questions or suggestions regarding this plugin. Code changes like bug fixes or new features can be proposed as pull requests.

## Releasing

There are a few steps to release a plugin version:

1. create a pull request and merge it
   --> The `draft-release.yml` will draft a release for you or add the pull request to an already existing draft release
1. Choose version, keep the pattern of the [semantig versioning](https://semver.org/) in mind. Adapt tag appropriately (eg. `v1.0.0`)   
1. Once you click on "publish release" a `publish-release.yml` will create an artifact using the tag as version (eg. `v1.0.0`-> `1.0.0`) and will publish it to the Gradle plugin portal.
