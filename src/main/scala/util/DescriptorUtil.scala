package util

import descriptor._

/**
 * Created by Junya on 15/05/24.
 */
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
