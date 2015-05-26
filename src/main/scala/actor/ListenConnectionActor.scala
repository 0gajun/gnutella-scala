package actor

import java.io.BufferedOutputStream
import java.net.{Socket, ServerSocket}
import java.util.Date

import actor.ListenConnectionActor.ListenConnection
import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import gnutella.Gnutella
import util.Logger

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Random}

/**
 * 外部からincomingConnectionを受け付けるActor
 * Created by Junya on 15/05/07.
 */
class ListenConnectionActor extends Actor {
  // TODO: This is for test
  private val listeningPort = 6666 + new Random(new Date().getTime).nextInt() % 1000
  private val serverSocket = new ServerSocket(listeningPort)
  private var manager: ActorRef = _

  override def receive: Receive = {
    case ListenConnection => listen()
  }

  /**
   * コネクションの待受を行なう
   */
  private def listen(): Unit = {
    val sock = serverSocket.accept()
    if (recvConnectionRequest(sock)) {
      Logger.info("incoming connection accepted!")
      manager ! RunConnectionActor(sock)
    } else {
      Logger.info("incoming connection is invalid. Close connection")
      sock.close()
    }
    self ! ListenConnection
  }


  /**
   * Gnutellaコネクションの接続要求を受付る
   * @param socket
   * @return requestが正しかった場合はtrue, 不正な場合はfalseを返す
   */
  private def recvConnectionRequest(socket: Socket): Boolean = {
    val source = scala.io.Source.fromInputStream(socket.getInputStream)

    // 改行が消えるので追加
    val msg = source.getLines().takeWhile(!_.isEmpty).mkString + "\n\n"
    msg match {
      case Gnutella.CONNECTION_REQUEST_MSG => replyOK(socket); true
      case _ => replyNO(socket); false
    }
  }

  private def replyOK(socket: Socket): Unit = {
    writeSocket(socket, Gnutella.CONNECTION_OK_MSG)
  }

  private def replyNO(socket: Socket): Unit = {
    writeSocket(socket, "GNUTELLA INVALID \n\n")
  }

  private def writeSocket(socket: Socket, msg: String): Unit = {
    val output = new BufferedOutputStream(socket.getOutputStream)
    output.write(msg.getBytes)
    output.flush()
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    super.preStart()
    implicit val timeout = Timeout(1000 nanos)

    Logger.debug("ListenConnectionActor start listen: port->" + listeningPort)

    //ConnectionManagerの取得
    val f = context.system.actorSelection("user/" + ConnectionManagerActor.name).resolveOne()
    f onComplete {
      case Success(ref) => manager = ref
      case Failure(t) => Logger.fatal("ListenConnectionActor cannot find ConnectionManagerActor")
    }
    Await.ready(f, 0 nanos)
  }
}

object ListenConnectionActor {
  val name = "listenConnection"

  case class ListenConnection()

}

