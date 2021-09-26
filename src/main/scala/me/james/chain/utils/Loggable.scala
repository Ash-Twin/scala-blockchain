package me.james.chain.utils

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {
  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass.getSimpleName)
}
