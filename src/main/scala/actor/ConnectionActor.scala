package actor

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.Socket

import akka.actor.{ActorRef, Actor}
import descriptor._
import interpreter.DescriptorInterpreter
import util.Logger

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
    case CreateConnectionActor(s) => onCreate(s)
    case "run" => run()
    case SendMessage(s) => sendMessage(s)
  }

  /**
   * セットアップ
   * @param s
   */
  private def onCreate(s: Socket): Unit = {
    Logger.debug("ConnectionActor created!")
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
    Logger.debug("send message")
    val byteArray = message.toByteArray()
    Logger.debug("byteArray->" + byteArray.length)
    if (output == null)
      Logger.fatal("Connection actor isn't set up.")
    output.write(byteArray)
    output.flush()
  }

  /**
   * コネクションからメッセージの待受を行う
   * メッセージ処理orタイムアウト後，自身に"run"メッセージを送る
   */
  private def run(): Unit = {
    checkConnectionStatus()

    readHeader match {
      case Some(header) => Logger.debug("message receive");onReceiveMessage(header)
      case None =>
    }
    self ! "run"
  }

  /**
   * コネクションがクローズされているかチェックを行い，クローズされている場合はActorをstopする
   */
  private def checkConnectionStatus(): Unit = {
    if (socket.isClosed) {
      input.close()
      output.close()
      context.stop(self)
    }
  }

  /**
   * メッセージを受信した時の処理を行う
   * @param header
   */
  private def onReceiveMessage(header: Array[Byte]): Unit = {
    Logger.info("message receive")
    val len = DescriptorHeader.calcPayloadLength(header)
    val payload = if (len > 0) readPayload(len).get else Array[Byte]()

    // メッセージのフォワーディングを行う.
    // 応答等は，引数として渡されたselfのメールボックスにメッセージを入れる形で処理する
    DescriptorInterpreter.execute(header, payload, context) match {
      case Some(s) => s match {
        case ping: PingDescriptor => Logger.info("receive ping"); manager ! BroadcastMessage(this, ping)
        case pong: PongDescriptor => Logger.info("receive pong"); manager ! ForwardMessage(pong)
        case query: QueryDescriptor => Logger.info("receive query"); manager ! BroadcastMessage(this, query)
        case hits: QueryHitsDescriptor => Logger.info("receive queryHits"); manager ! ForwardMessage(hits)
      }
      case None => Logger.error("unknown descriptor type @onReceiveMessage")
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