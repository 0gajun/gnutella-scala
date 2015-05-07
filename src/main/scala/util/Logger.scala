package util

/**
 * ログを吐くためのラッパークラス
 * Created by Junya on 15/05/02.
 */
object Logger {

  def info(msg: String): Unit = {
    println("info: " + msg)
  }

  def error(msg: String): Unit = {
    println("[[[Error]]]: " + msg)
  }

  def debug(msg: String): Unit = {
    println("===Debug===: " + msg)
  }

  def fatal(msg: String): Unit = {
    println("!!!!fatal!!!!")
    println(msg)
    sys.exit(1)
  }

}
