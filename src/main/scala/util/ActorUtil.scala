package util

import akka.actor.{ActorSelection, ActorRef, ActorSystem}

/**
 * Actor周りを便利に使うためのUtil
 * Created by Junya on 15/05/14.
 */
object ActorUtil {

  def getActor(system: ActorSystem, actorName: String): ActorSelection = {
    system.actorSelection("user/" + actorName)
  }

}
