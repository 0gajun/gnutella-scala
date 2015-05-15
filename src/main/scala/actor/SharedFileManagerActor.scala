package actor

import java.io.File

import actor.SharedFileManagerActor.{FileInfo, FileSearch, Initialize, RegisterNewFile}
import akka.actor.Actor
import util.Logger

import scala.collection.mutable

/**
 * Gnutellaクライアントにおける共有ファイルを管理するアクター
 * Created by Junya on 15/05/11.
 */
class SharedFileManagerActor extends Actor {


  private val DEFAULT_SHARED_FOLDER_PATH = "./shared/"
  private val rootFolder = new File(DEFAULT_SHARED_FOLDER_PATH)

  private[this] val fileEntries = new mutable.ListBuffer[FileInfo]()

  override def receive: Receive = {
    case Initialize => initialize()
    case FileSearch(f, b) => sender ! search(f, b)
    case RegisterNewFile(f) => registrationRequestHandler(f)
  }

  /**
   * 初期化を行なう
   */
  private def initialize(): Unit = {
    updateFileEntry()
  }

  /**
   * ファイルエントリ一覧を更新する
   */
  private def updateFileEntry(): Unit = {
    if (!rootFolder.exists()) {
      // TODO: 共有フォルダが存在しない場合の処理
    }
    if (!rootFolder.isDirectory) {
      // TODO: 共有フォルダをユーザが自由に選べるようになった際に，そのフォルダが本当にディレクトリかどうかの確認
      // 異なればもう一度選ばせる
    }

    fileEntries.clear()
    recurDir(rootFolder)
  }

  /**
   * ディレクトリを再帰的に探索する
   * @param dir ディレクトリのファイルオブジェクト
   */
  private def recurDir(dir: File): Unit = {
    dir.listFiles().foreach {
      case f if f.isFile => registerFileToFileEntry(f)
      case d if d.isDirectory => recurDir(d)
      case other => Logger.error("unknown type: path-> " + other.getAbsolutePath)
    }
  }

  /**
   * ファイルエントリを登録する
   * @param file 登録するファイルオブジェクト
   */
  private def registerFileToFileEntry(file: File): Unit = {
    fileEntries += ((file.getName, file.length(), file.getAbsolutePath, fileEntries.length))
    Logger.debug("file(" + file.getName + ") is registered")
  }

  /**
   * ファイルの登録リクエストを処理する. (バリデーションチェック
   */
  private def registrationRequestHandler(file: File): Unit = {
    if (file.isFile && file.getAbsolutePath.contains(rootFolder.getAbsolutePath)) {
      registerFileToFileEntry(file)
    }
  }

  /**
   * スペース区切りの複数条件ファイル検索を行なう
   * ##現状，先頭の条件のみ##，
   * @param query
   * @return
   */
  private def search(query: String, isLikeSearch: Boolean): List[FileInfo] = {
    val fileNames = query.split(' ')
    searchFile(fileNames(0), true)
  }

  /**
   * ファイルの検索を行なう
   * @param fileName 検索対象のファイル名
   * @param isLikeSearch 部分一致を行なうか否か．行なう場合はtrue
   * @return 検索結果のリスト
   */
  private def searchFile(fileName: String, isLikeSearch: Boolean): List[FileInfo] = {
    isLikeSearch match {
      case false => exactSearchFile(fileName)
      case true => likeSearchFile(fileName)
    }
  }

  /**
   * ファイル名との完全一致検索を行う
   * @param fileName 検索を行いたいファイル名
   * @return 検索結果のリスト
   */
  private def exactSearchFile(fileName: String): List[FileInfo] = {
    fileEntries filter (_._1 == fileName) toList
  }

  /**
   * ファイル名の部分一致検索を行う
   * @param query 部分一致を行うクエリー
   * @return 検索結果のリスト
   */
  private def likeSearchFile(query: String): List[FileInfo] = {
    val reg = ".*" + query + ".*"
    fileEntries filter (_._1.matches(reg)) toList
  }

}

object SharedFileManagerActor {
  // Tuple3(FileName, FileSize, FilePath, FileIndex)
  type FileInfo = (String, Int, String, Int)

  val name = "sharedFileManager"

  case class Initialize()
  case class RegisterNewFile(file: File)
  case class FileSearch(fileName: String, isLikeSearch: Boolean)

}
