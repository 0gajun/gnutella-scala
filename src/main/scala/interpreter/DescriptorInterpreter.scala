package interpreter


import descriptor.DescriptorHeader
import util.Logger

/**
 * ConnectionActorより渡されるDescriptorのバイト列を解釈するクラス
 * Created by Junya on 15/05/03.
 */
object DescriptorInterpreter {
  def execute(header: Array[Byte], payload: Array[Byte]): Option[DescriptorHeader] = {
    header(DescriptorHeader.payloadDescriptorOffset.toInt) match {
      case DescriptorHeader.P_DESC_PING => PingInterpreter.execute(header, payload)
      case DescriptorHeader.P_DESC_PONG =>
      case DescriptorHeader.P_DESC_QUERY =>
      case DescriptorHeader.P_DESC_QUERY_HITS =>
      case DescriptorHeader.P_DESC_PUSH =>
      case _ => Logger.error("Unknown payloadDescriptor type. discard."); None
    }
  }
}
