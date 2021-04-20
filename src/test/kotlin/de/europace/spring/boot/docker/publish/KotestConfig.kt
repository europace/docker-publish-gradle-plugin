package de.europace.spring.boot.docker.publish

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode

class KotestConfig : AbstractProjectConfig() {

  override val isolationMode = IsolationMode.InstancePerLeaf
}
