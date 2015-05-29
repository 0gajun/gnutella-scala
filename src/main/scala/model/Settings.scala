package model

import java.util.Random

import util.Logger

/**
 * Created by Junya on 15/05/28.
 */
object Settings {

  val FILE_SERVER_PORT = 18080 + new Random().nextInt() % 1000 // FOR TEST

  val LOG_LEVEL = Logger.NONE


  val DEFAULT_SHARED_FOLDER_PATH = "./shared/"
}
