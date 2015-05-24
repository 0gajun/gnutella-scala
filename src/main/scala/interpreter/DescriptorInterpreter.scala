package interpreter


import akka.actor.{ActorContext, ActorRef}
import descriptor.DescriptorHeader
import util.Logger

/**
 * ConnectionActorより渡されるDescriptorのバイト列を解釈するクラス
 * Created by Junya on 15/05/03.
 */
object DescriptorInterpreter {
  def execute(header: Array[Byte], payload: Array[Byte], callerContext: ActorContext): Option[DescriptorHeader] = {
    DescriptorHeader.calcPayloadDescriptor(header) match {
      case DescriptorHeader.P_DESC_PING => PingInterpreter.execute(header, payload, callerContext)
      case DescriptorHeader.P_DESC_PONG => PongInterpreter.execute(header, payload, callerContext)
      case DescriptorHeader.P_DESC_QUERY => QueryInterpreter.execute(header, payload, callerContext)
      case DescriptorHeader.P_DESC_QUERY_HITS => QueryHitsInterpreter.execute(header, payload, callerContext)
      case DescriptorHeader.P_DESC_PUSH => Logger.info("not implemented"); None
      case value => Logger.error("Unknown payloadDescriptor type(" + value + "). discard."); None
    }
  }
}
