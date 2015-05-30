package util

import java.net.{Inet6Address, NetworkInterface, InetAddress}

import akka.actor.{ActorSelection, ActorSystem}
import descriptor._

/**
 * Created by Junya on 15/05/30.
 */
object GnutellaUtil {

}

object DescriptorUtil {
  def getTypeStringOfDescriptor(desc: DescriptorHeader): String = {
    desc match {
      case ping: PingDescriptor => "Ping"
      case pong: PongDescriptor => "Pong"
      case query: QueryDescriptor => "Query"
      case queryHits: QueryHitsDescriptor => "QueryHits"
      case _ => "Unknown"
    }
  }
}



/**
 * Actor周りを便利に使うためのUtil
 * Created by Junya on 15/05/14.
 */
object ActorUtil {

  def getActor(system: ActorSystem, actorName: String): ActorSelection = {
    system.actorSelection("user/" + actorName)
  }

}

object NetworkUtil {

  /**
   * 自ホストのローカルループバックアドレスでない，最初のアドレスを取得する
   * @return
   */
  def getMyAddress: Option[InetAddress] = {
    val nics = NetworkInterface.getNetworkInterfaces
    while (nics.hasMoreElements) {
      val nic = nics.nextElement()
      val addrs = nic.getInetAddresses
      while (addrs.hasMoreElements) {
        val addr = addrs.nextElement()
        if (!addr.isInstanceOf[Inet6Address] && !addr.getHostAddress.equals("127.0.0.1"))
          return Option(addr)
      }
    }
    None
  }
}