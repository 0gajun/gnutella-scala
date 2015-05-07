package util

/**
 * Created by Junya on 15/05/02.
 */
object Logger {

  def info(msg: String): Unit = {
    println("Logger: " + msg)
  }

  def error(msg: String): Unit = {
    println("[[[Error]]]: " + msg)
  }

}
