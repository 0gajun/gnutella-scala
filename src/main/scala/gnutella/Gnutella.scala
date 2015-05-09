package gnutella

import java.net.{Socket, InetAddress}

import actor._
import akka.actor.{ActorRef, Props, ActorSystem}
import descriptor.PingDescriptor
import util.Logger

import scala.util.Try

/**
 *
 * Created by Junya on 15/05/07.
 */
object Gnutella {
  private[this] var gnutellaStatus: Int = GnutellaStatus.initializing

  def getStatus = gnutellaStatus

  def isWaitingPong = { gnutellaStatus == GnutellaStatus.waitingPong }

  def main(args: Array[String]): Unit = {
    println("Welcome to gnutella!!!")
    setUp()

  }


  private def setUp(): Unit = {
    val connectionManager = setUpActors()

    println("Connect other servent?(y/n)")
    val c = scala.io.StdIn.readChar()
    if (c.equals('n'))
      return

    setUpFirstConnection(connectionManager)
    gnutellaStatus = GnutellaStatus.waitingPong
    sendPing(connectionManager)
    Logger.info("setup finished")
  }

  /**
   *
   * @return ConnectionManager
   */
  private def setUpActors(): ActorRef = {
    val system = ActorSystem("gnutellaActorSystem")
    val manager = system.actorOf(Props[ConnectionManagerActor], ConnectionManagerActor.name)
    system.actorOf(Props[ServentsManagerActor], ServentsManagerActor.name)
    val listen = system.actorOf(Props[ListenConnectionActor], ListenConnectionActor.name)
    listen ! ListenConnectionActor.ListenConnection

    manager
  }

  private def setUpFirstConnection(connectionManager: ActorRef): Unit = {
    print("please input IPAddress: ")
    val ipAddress = InetAddress.getByName(scala.io.StdIn.readLine())
    print("please input port: ")
    val port = scala.io.StdIn.readShort()

    Try( new Socket(ipAddress, port) ).toOption match {
      case Some(s) => connectionManager ! RunConnectionActor(s)
      case None => fatal("cannot create socket.")
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
