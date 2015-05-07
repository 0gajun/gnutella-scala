package actor

import actor.ServentsManagerActor.{ResetTable, GetServents, RegisterServent}
import akka.actor.Actor
import model.ServentInfo
import util.Logger

import scala.collection.mutable.ArrayBuffer

/**
 * Pong応答が返って来たServent一覧を管理するActor
 * Created by Junya on 15/05/07.
 */
class ServentsManagerActor extends Actor {

  private[this] val serventList = ArrayBuffer.empty[ServentInfo]

  override def receive: Receive = {
    case RegisterServent(servent) => registerServent(servent)
    case GetServents => sender ! List(serventList)
    case ResetTable => resetTable()
  }

  private def resetTable(): Unit = {
    serventList.clear()
  }

  private def registerServent(servent: ServentInfo) = {
    Logger.info("servent was registered. (ip: " + servent.ip + ")")
    serventList+=servent
  }
}

object ServentsManagerActor {
  val name = "serventsManager"

  case class ResetTable()
  case class RegisterServent(servent: ServentInfo)
  case class GetServents()
}
