package actor

import java.net.Socket

import akka.actor.Actor
import akka.actor.Actor.Receive
import descriptor.DescriptorHeader

/**
 * Created by Junya on 15/05/01.
 */
class ConnectionActor extends Actor {
  private[this] var socket: Socket = _


  override def receive: Receive = {
    case CreateConnectionActor(s) => onCreate(_)
    case "run" => run()
    case SendMessage => sendMessage(_)
  }

  private def onCreate(s: Socket): Unit = {
    socket = s
  }

  private def sendMessage(message: DescriptorHeader) = {

  }

  private def run(): Unit = {

  }
}

case class SendMessage(message: DescriptorHeader)
case class CreateConnectionActor(socket: Socket)