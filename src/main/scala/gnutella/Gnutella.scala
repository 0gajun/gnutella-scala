package gnutella

import java.io.BufferedOutputStream
import java.net.{Socket, InetAddress}
import java.util.UUID

import actor._
import akka.actor.{ActorRef, Props, ActorSystem}
import descriptor.{QueryDescriptor, PingDescriptor}
import util.Logger

import scala.util.Try

/**
 *
 * Created by Junya on 15/05/07.
 */
object Gnutella {

  val CONNECTION_REQUEST_MSG = "GNUTELLA CONNECT/0.4\n\n"
  val CONNECTION_OK_MSG = "GNUTELLA OK\n\n"

  private[this] var gnutellaStatus: Int = GnutellaStatus.initializing
  private[this] val serventIdentifier: String = UUID.randomUUID().toString.replace("-", "")

  private[this] var connectionManager: ActorRef = _

  def getServentIdentifier = serventIdentifier

  def getStatus = gnutellaStatus

  def isWaitingPong = gnutellaStatus == GnutellaStatus.waitingPong

  def main(args: Array[String]): Unit = {
    println("Welcome to gnutella!!!")
    setUp()

    println("sendQuery?(y/n)")
    if (scala.io.StdIn.readChar().equals('y')) {
      val query = new QueryDescriptor
      query.minimumSpeed = 0
      query.searchCriteria = "test"
      gnutellaStatus = GnutellaStatus.waitingQueryHits
      connectionManager ! SendMessageToAllConnections(query)
    }
  }


  private def setUp(): Unit = {
    val connectionManager = setUpActors()

    println("Connect other servent?(y/n)")
    val c = scala.io.StdIn.readChar()
    if (c.equals('n'))
      return

    print("please input IPAddress: ")
    val ipAddress = InetAddress.getByName(scala.io.StdIn.readLine())
    print("please input port: ")
    val port = scala.io.StdIn.readInt()

    setUpFirstConnection(ipAddress, port)
    gnutellaStatus = GnutellaStatus.waitingPong
    sendPing(connectionManager)

    // Pingレスポンスの待機時間
    Thread.sleep(3000)
    gnutellaStatus = GnutellaStatus.running
    Logger.info("setup finished")
  }

  /**
   * 本アプリケーションで使用するActorを起動する(ConnectionActor以外)
   * @return ConnectionManager
   */
  private def setUpActors(): ActorRef = {
    val system = ActorSystem("gnutellaActorSystem")
    val manager = system.actorOf(Props[ConnectionManagerActor], ConnectionManagerActor.name)
    val listen = system.actorOf(Props[ListenConnectionActor], ListenConnectionActor.name)
    val fileManager = system.actorOf(Props[SharedFileManagerActor], SharedFileManagerActor.name)
    val fileServer = system.actorOf(Props[FileServerActor], FileServerActor.name)
    system.actorOf(Props[ServentsManagerActor], ServentsManagerActor.name)

    listen ! ListenConnectionActor.ListenConnection
    fileManager ! SharedFileManagerActor.Initialize
    fileServer ! FileServerActor.Listen

    connectionManager = manager
    manager
  }

  private def setUpFirstConnection(ipAddress: InetAddress, port: Int): Unit = {
    Try(new Socket(ipAddress, port)).toOption match {
      case Some(s) =>
        negotiateConnection(s) match {
          case None => connectionManager ! RunConnectionActor(s)
          case Some(msg) => Logger.debug(msg)
        }
      case None => fatal("cannot create socket.")
    }
  }

  /**
   * 初回接続要求を行なう
   * @param socket 接続対象のソケット
   * @return エラーが生じた場合はエラーメッセージを返す.正常終了はNone
   */
  private def negotiateConnection(socket: Socket): Option[String] = {
    requestConnection(socket)
    recvRespondToReq(socket)
  }

  /**
   * 接続要求メッセージを送信する
   * @param socket 接続対象のソケット
   */
  private def requestConnection(socket: Socket): Unit = {
    val output = new BufferedOutputStream(socket.getOutputStream)

    output.write(Gnutella.CONNECTION_REQUEST_MSG.getBytes)
    output.flush()
  }

  /**
   * 接続要求に対する応答を受信する
   * @param sock 接続要求を送ったソケット
   * @return エラーが生じた場合はエラーメッセージを返す．正常終了はNone
   */
  private def recvRespondToReq(sock: Socket): Option[String] = {
    val input = scala.io.Source.fromInputStream(sock.getInputStream)

    // 改行が消えるので追加する
    Try(input.getLines().takeWhile(!_.isEmpty).mkString + "\n\n").toOption match {
      case Some(s) =>
        s match {
          case CONNECTION_OK_MSG => None
          case msg => Option("Cannot connect to the servent which you specified. Message->" + msg)
          case _ => Logger.fatal("Unknown Error@recvRespondToReq"); Option("Unknown")
        }
      case None => Option("timeout negotiation.")
    }
  }

  private def sendPing(connectionManager: ActorRef): Unit = {
    val ping = new PingDescriptor
    ping.ttl = 7
    ping.hops = 0
    connectionManager ! SendMessageToAllConnections(ping)
  }

  private def fatal(msg: String): Unit = {
    println("fatal!!!!!!!!!!!!\n" + msg)
    System.exit(1)
  }

}

object GnutellaStatus {
  val initializing = 1
  val waitingPong = 2
  val running = 3
  val waitingQueryHits = 4
}
