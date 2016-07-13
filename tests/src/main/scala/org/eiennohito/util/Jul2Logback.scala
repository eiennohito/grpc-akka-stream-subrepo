package org.eiennohito.util

import java.util.logging.{Level, LogManager, Logger}

import org.slf4j.bridge.SLF4JBridgeHandler

/**
  * @author eiennohito
  * @since 2016/04/29
  */
object Jul2Logback {
  val initialized = {
    LogManager.getLogManager.reset()
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    Logger.getGlobal.setLevel(Level.FINEST)
    true
  }
}
