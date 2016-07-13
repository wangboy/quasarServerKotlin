package logger

import org.apache.log4j.Logger
import org.apache.log4j.xml.DOMConfigurator

/**
 * Created by wangbo on 16/7/8.
 */

fun initLogger() {
    DOMConfigurator.configure("log4j.xml")
}

val slogger: Logger = Logger.getLogger("Server")
val clogger: Logger = Logger.getLogger("Client")
val mlogger: Logger = Logger.getLogger("Message")