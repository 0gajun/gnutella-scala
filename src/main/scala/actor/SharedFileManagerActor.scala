package actor

import java.io.File

import akka.actor.Actor
import akka.actor.Actor.Receive
import util.Logger

import scala.collection.mutable.HashMap

/**
 * Gnutellaクライアントにおける共有ファイルを管理するアクター
 * Created by Junya on 15/05/11.
 */
class SharedFileManagerActor extends Actor {

  private val DEFAULT_SHARED_FOLDER_PATH = "./shared/"

  // FileName->(FileSize, FilePath)
  private[this] val fileEntries = new HashMap[String, (Long, String)]()

  override def receive: Receive = {
    case SharedFileManagerActor.Initialize => initialize()
  }

  private def initialize(): Unit = {
    updateFileEntry()
  }


  private def updateFileEntry(): Unit = {
    val rootFolder = new File(DEFAULT_SHARED_FOLDER_PATH)
    if (!rootFolder.exists()) {
      // TODO: 共有フォルダが存在しない場合の処理
    }
    if (!rootFolder.isDirectory) {
      // TODO: 共有フォルダをユーザが自由に選べるようになった際に，そのフォルダが本当にディレクトリかどうかの確認
      // 異なればもう一度選ばせる
    }

    fileEntries.clear()
    searchDir(rootFolder)
  }

  private def searchDir(dir: File): Unit = {
    Logger.debug("directory name: " + dir.getName + " @searchDir")
    dir.listFiles().foreach {
      case f if f.isFile => registerFileToFileEntry(f)
      case d if d.isDirectory => searchDir(d)
      case other => Logger.error("unknown type: path-> " + other.getAbsolutePath)
    }
  }

  private def registerFileToFileEntry(file: File): Unit = {
    fileEntries+=(file.getName->(file.length(), file.getAbsolutePath))
    Logger.debug("file(" + file.getName + ") is registered")
  }

}

object SharedFileManagerActor {
  val name = "sharedFileManager"

  case class Initialize()
}
