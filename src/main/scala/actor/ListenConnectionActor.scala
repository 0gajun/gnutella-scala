package actor

import java.net.ServerSocket

import akka.actor.Actor

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
    val socket = serverSocket.accept()
    val manager = context.actorSelection("user/" + ConnectionManagerActor.name)
    manager ! RunConnectionActor(socket)
    self ! ListenConnection
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
  }

  case class ListenConnection()
}

