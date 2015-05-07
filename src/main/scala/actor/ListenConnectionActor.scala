package actor

import java.net.ServerSocket

import actor.ListenConnectionActor.ListenConnection
import akka.actor.{ActorSystem, Actor}
import util.Logger

/**
 * 外部からincomingConnectionを受け付けるActor
 * Created by Junya on 15/05/07.
 */
class ListenConnectionActor extends Actor {
  private val listeningPort = 6666
  private val serverSocket = new ServerSocket(listeningPort)

  override def receive: Receive = {
    case ListenConnection => listen()
  }

  private def listen(): Unit = {
    Logger.debug("ListenConnectionActor start listen")
    val socket = serverSocket.accept()
    val manager = context.system.actorSelection("user/" + ConnectionManagerActor.name)
    manager ! RunConnectionActor(socket)
    Logger.info("incoming connection accepted!")
    self ! ListenConnection
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
  }

}

object ListenConnectionActor {
  val name = "listenConnection"
  case class ListenConnection()
}

