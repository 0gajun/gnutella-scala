package actor

import java.io._
import java.net.{Socket, InetAddress}
import java.nio.file.Files

import actor.DownloaderActor.Download
import akka.actor.Actor
import akka.actor.Actor.Receive
import model.Settings
import util.Logger

import scala.util.Try

/**
 * ファイルダウンロードを行なうアクター
 * Created by Junya on 15/05/26.
 */
class DownloaderActor extends Actor {

  private val FILE_RECV_BUF_SIZE = 1024

  override def receive: Receive = {
    case Download(ip, port, fileIndex, fileName) => download(ip, port, fileIndex, fileName)
  }

  private def download(ip: InetAddress, port: Int, fileIndex: Int, fileName: String): Unit = {
    Try(new Socket(ip, port)).toOption match {
      case Some(s) =>
        requestDownload(s, fileIndex, fileName, 0) //TODO: Resumeに対応
        recvResponse(s, fileName, 0)
      case None =>
    }
  }

  private def requestDownload(socket: Socket, fileIndex: Int, fileName: String, offset: Long): Unit = {
    val msg = "GET /get/${fileIndex}/${fileName}/ HTTP/1.0\r\n" +
      "User-Agent: Gnutella/0.4\r\n" +
      "Range: bytes=${offset}-\r\n" +
      "Connection: Keep-Alive\r\n" +
      "\r\n"

    val output = new BufferedOutputStream(socket.getOutputStream)
    output.write(msg.getBytes)
    output.flush()
  }

  private def recvResponse(socket: Socket, fileName: String, offset: Long): Unit = {
    val responseLines = scala.io.Source.fromInputStream(socket.getInputStream)
      .getLines().takeWhile(!_.isEmpty).toList

    responseLines.head match {
      case "HTTP/1.0 200 OK" => Logger.debug("Download request is accepted!")
      case _ => // TODO: ErrorHandling
    }

    val headers = responseLines.tail.foldLeft(Map.empty[String, String])(_ ++ parseHeader(_))

    headers.get("Content-Length") match {
      case Some(len) => Try(len.toLong).toOption match {
        case Some(contentLen) => recvData(socket, fileName, contentLen)
        case None => // TODO: ErrorHandling
      }
      case _ => //TODO: ErrorHandling
    }
  }

  /**
   * データを受信して，ファイルに保存する
   * @param socket
   * @param fileName
   * @param contentLen
   */
  private def recvData(socket: Socket, fileName: String, contentLen: Long): Unit = {
    val input = new BufferedInputStream(socket.getInputStream)
    val output = new BufferedOutputStream(new FileOutputStream(Settings.DEFAULT_SHARED_FOLDER_PATH + fileName))

    val buf = new Array[Byte](FILE_RECV_BUF_SIZE)

    var recvedSize = 0

    do {
      val size = input.read(buf)
      recvedSize+=size
      output.write(buf, 0, size)
    } while (recvedSize < contentLen)

    output.flush()
  }

  private def parseHeader(line: String): Map[String, String] = {
    val pair = line.split(":")
    Map(pair(0) -> pair(1))
  }

}

object DownloaderActor {

  case class Download(ip: InetAddress, port: Int, fileIndex: Int, fileName: String)

}
