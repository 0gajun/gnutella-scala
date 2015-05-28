package actor

import java.io._
import java.net.{Socket, InetAddress}

import actor.DownloaderActor.Download
import akka.actor.{Kill, Actor}
import model.Settings
import util.Logger

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * ファイルダウンロードを行なうアクター
 * Created by Junya on 15/05/26.
 */
class DownloaderActor extends Actor {

  private val FILE_RECV_BUF_SIZE = 1024

  private var input: InputStream = _
  private var output: OutputStream = _
  private var socket: Socket = _

  override def receive: Receive = {
    case Download(ip, port, fileIndex, fileName) => download(ip, port, fileIndex, fileName)
  }

  private def download(ip: InetAddress, port: Int, fileIndex: Int, fileName: String): Unit = {
    Try(new Socket(ip, port)).toOption match {
      case Some(s) =>
        input = s.getInputStream
        output = s.getOutputStream
        socket = s
        requestDownload(fileIndex, fileName, 0) //TODO: Resumeに対応
        val contentLen = recvResponse(fileName, 0)

        if (contentLen > 0) {
          recvData(fileName, contentLen)
        }
      case None => Logger.debug("cannot open socket@download")
    }
    socket.close()

    self ! Kill
  }

  private def requestDownload(fileIndex: Int, fileName: String, offset: Long): Unit = {
    val msg = s"GET /get/${fileIndex}/${fileName}/ HTTP/1.0\r\n" +
      "User-Agent: Gnutella/0.4\r\n" +
      s"Range: bytes=${offset}-\r\n" +
      "Connection: Keep-Alive\r\n" +
      "\r\n"

    output.write(msg.getBytes)
    output.flush()
  }

  private def recvResponse(fileName: String, offset: Long): Long = {
    val responseLines = new ArrayBuffer[String]()

    // ヘッダ部分のみを読み込むために,あえてInputStreamをラップしないで読み込む
    // 後のデータ受信の為
    var line: String = ""
    while ({ line = readLine(); !line.isEmpty }) {
      responseLines+=line
    }

    responseLines.head match {
      case "HTTP/1.0 200 OK" => Logger.debug("Download request is accepted!")
      case msg => Logger.info("RequestRefused! Message->" + msg); return -1
    }

    val headers = responseLines.tail.foldLeft(Map.empty[String, String])(_ ++ parseHeader(_))

    headers.get("Content-Length") match {
      case Some(len) => Try(len.toLong).toOption match {
        case Some(contentLen) => contentLen
        case None => Logger.info("Invalid format response received..."); -1
      }
      case _ => Logger.info("Invalid format response received..."); -1
    }
  }

  /**
   * InputStreamから一文字ずつ読み込みを行い，1行分を受信して返す
   * @return
   */
  private def readLine(): String = {
    var data: Int = 0
    var buf = ArrayBuffer[Byte]()
    while ({ data = input.read(); data != '\n'}) {
      if (data != '\r') {
        buf+=data.toByte
      }
    }
    new String(buf.toArray)
  }

  /**
   * データを受信して，ファイルに保存する
   * @param fileName
   * @param contentLen
   */
  private def recvData(fileName: String, contentLen: Long): Unit = {
    val input = new BufferedInputStream(this.input)
    val output = new BufferedOutputStream(new FileOutputStream(Settings.DEFAULT_SHARED_FOLDER_PATH + "testRecv")) //fileName))
    val buf = new Array[Byte](FILE_RECV_BUF_SIZE)

    var size = 0

    while ({ size = input.read(buf); size != -1 }) {
      output.write(buf, 0, size)
    }

    output.flush()
    Logger.info("Download completed!->" + fileName)
  }

  private def parseHeader(line: String): Map[String, String] = {
    val pair = line.replace(" ", "").split(":")
    Map(pair(0) -> pair(1))
  }

}

object DownloaderActor {

  case class Download(ip: InetAddress, port: Int, fileIndex: Int, fileName: String)

}
