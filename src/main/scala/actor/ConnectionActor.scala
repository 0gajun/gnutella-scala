package actor

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.Socket

import akka.actor.{ActorRef, Actor}
import descriptor._
import interpreter.DescriptorInterpreter

import scala.util.Try

/**
 * 個々のコネクションにおけるメッセージの処理を行うActor
 * Created by Junya on 15/05/01.
 */
class ConnectionActor extends Actor {
  private[this] val timeoutInterval = 10

  private[this] var socket: Socket = _
  private[this] var input: BufferedInputStream = _
  private[this] var output: BufferedOutputStream = _

  private[this] val manager: ActorRef = context.parent

  override def receive: Receive = {
    case CreateConnectionActor(s) => onCreate(_)
    case "run" => run()
    case SendMessage => sendMessage(_)
  }

  /**
   * セットアップ
   * @param s
   */
  private def onCreate(s: Socket): Unit = {
    socket = s
    socket.setSoTimeout(timeoutInterval)
    input = new BufferedInputStream(s.getInputStream)
    output = new BufferedOutputStream(socket.getOutputStream)
  }

  /**
   * このアクターが保持するコネクションに対してメッセージを送る
   * @param message
   */
  private def sendMessage(message: DescriptorHeader) = {
    val byteArray = message.toByteArray()
    output.write(byteArray)
    output.flush()
  }

  /**
   * コネクションからメッセージの待受を行う
   * メッセージ処理orタイムアウト後，自身に"run"メッセージを送る
   */
  private def run(): Unit = {
    readHeader match {
      case Some(header) => onReceiveMessage(header)
    }
    self ! "run"
  }

  /**
   * メッセージを受信した時の処理を行う
   * @param header
   */
  private def onReceiveMessage(header: Array[Byte]): Unit = {
    val len = DescriptorHeader.calcPayloadLength(header)
    val payload = if (len > 0) readPayload(len).get else Array[Byte]()

    DescriptorInterpreter.execute(header, payload) match {
      case Some(s) => s match {
        case ping: PingDescriptor => manager ! BroadcastMessage(this, ping)
        case pong: PongDescriptor => manager ! ForwardMessage(pong)
        case query: QueryDescriptor => manager ! BroadcastMessage(this, query)
        case hits: QueryHitsDescriptor => manager ! ForwardMessage(hits)
      }
      case None =>
    }
  }

  /**
   * Descriptorのヘッダを読み込む
   * @return
   */
  private def readHeader: Option[Array[Byte]] = {
    getData(DescriptorHeader.headerSize)
  }

  /**
   * 指定サイズ分ペイロードを読み込む
   * @param size
   * @return
   */
  private def readPayload(size: Int): Option[Array[Byte]] = {
    getData(size)
  }

  /**
   * ソケットより指定サイズ分読み込みを行う
   * @param size
   * @return
   */
  private def getData(size: Int): Option[Array[Byte]] = {
    val buf = new Array[Byte](size)
    Try ( input.read(buf, 0, size) ).toOption match {
      case Some(n) =>
        if (n == -1)
          None
        else
          Option(buf)
      case None => None
    }
  }
}

case class SendMessage(message: DescriptorHeader)
case class CreateConnectionActor(socket: Socket)