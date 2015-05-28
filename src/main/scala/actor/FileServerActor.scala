package actor

import java.io.{FileInputStream, BufferedInputStream, File, BufferedOutputStream}
import java.net.{Socket, ServerSocket}
import java.util.concurrent.TimeUnit

import actor.FileServerActor._
import actor.SharedFileManagerActor.{FileInfo, FindByIndex, FileSearch}
import akka.actor.Actor
import akka.pattern._
import akka.util.Timeout
import model.Settings
import util.{Logger, ActorUtil}

import scala.util.{Success, Try}

/**
 * ファイル取得要求を受け付けて送信するアクター
 * Created by Junya on 15/05/26.
 */
class FileServerActor extends Actor {

  private val serverSocket = new ServerSocket(Settings.FILE_SERVER_PORT)
  private val FILE_BUFFER_SIZE = 1024

  override def receive: Receive = {
    case Listen => listen()
  }

  private def listen() = {
    Try(serverSocket.accept()).toOption match {
      case Some(s) => recvRequest(s)
      case None =>
    }
    self ! Listen
  }

  private def recvRequest(socket: Socket): Unit = {
    val reqLineRegx = """GET /get/(\d+)/([a-zA-Z0-9.-_]+)/ HTTP/1.0""".r("fileIndex", "fileName") //TODO: 正規表現修正
    val request = scala.io.Source.fromInputStream(socket.getInputStream)
        .getLines()
        .takeWhile(!_.isEmpty)
        .mkString

    Logger.debug("recv file request->" + request)

    reqLineRegx.findFirstMatchIn(request) match {
      case Some(m) => sendRequestedFile(socket, Integer.parseInt(m.group("fileIndex")), m.group("fileName")) //値のオーバーフロー処理
      case None => respondToInvalidRequest(socket)
    }
  }

  private def sendRequestedFile(socket: Socket, fileIndex: Int, fileName: String): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    val fileManager = ActorUtil.getActor(context.system, SharedFileManagerActor.name)

    val f = fileManager ? FindByIndex(fileIndex)

    f.onComplete {
      case Success(res) => res match {
        case Some(s) => s match {
          case fileInfo: SharedFileManagerActor.FileInfo =>
            // indexとファイル名が一致するか確認
            if (!fileInfo._1.equals(fileName)) {
              respondToInvalidRequest(socket)
            }

            sendFile(socket, fileInfo)

            socket.close()

          case _ => Logger.fatal("unknown type @FileServerActor#sendRequestedFile()")
        }
        case None => respondFileNotFound(socket)
      }
      case _ => respondFileNotFound(socket) // File not found
    }
  }

  private def sendFile(socket: Socket, fileInfo: FileInfo): Unit = {
    val file = new File(fileInfo._3)

    if (!file.exists()) {
      respondFileNotFound(socket)
      return
    }

    val input = new BufferedInputStream(new FileInputStream(file))
    val output = new BufferedOutputStream(socket.getOutputStream)
    val buf = new Array[Byte](FILE_BUFFER_SIZE)

    val head =
      "HTTP/1.0 200 OK\r\n" +
        "Server: Gnutella/0.4\r\n" +
        "Content-Type: application/binary\r\n" +
        "Content-Length: " + file.length() + "\r\n" +
        "\r\n"
    output.write(head.getBytes)

    var size = 0
    while ({ size = input.read(buf); size != -1 }) {
      Logger.debug("write to socket, size->" + size)
      output.write(buf, 0, size)
    }
    output.flush()
  }

  private def respondToInvalidRequest(socket: Socket): Unit = {
    respond(socket, "HTTP/1.0 400 Bad Request\r\n")
    socket.close()
  }

  private def respondFileNotFound(socket: Socket): Unit = {
    respond(socket, "HTTP/1.0 404 Not Found\r\n")
    socket.close()
  }

  private def respond(socket: Socket, msg: String): Unit = {
    val output = new BufferedOutputStream(socket.getOutputStream)
    output.write(msg.getBytes)
    output.flush()
  }
}

object FileServerActor {
  val name = "FileServerActor"

  case class Listen()

}

