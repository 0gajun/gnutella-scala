package interpreter


import descriptor.DescriptorHeader

/**
 * ConnectionActorより渡されるDescriptorのバイト列を解釈するクラス
 * Created by Junya on 15/05/03.
 */
object DescriptorInterpreter {

  def execute(header: Array[Byte], payload: Array[Byte]): Unit = {
    header(DescriptorHeader.payloadDescriptorOffset.toInt) match {
      case DescriptorHeader.P_DESC_PING =>
      case DescriptorHeader.P_DESC_PONG =>
      case DescriptorHeader.P_DESC_QUERY =>
      case DescriptorHeader.P_DESC_QUERY_HITS =>
      case DescriptorHeader.P_DESC_PUSH =>
    }

  }
}
