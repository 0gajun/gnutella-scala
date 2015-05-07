package gnutella

/**
 *
 * Created by Junya on 15/05/07.
 */
object Gnutella {
  private[this] var gnutellaStatus: Int = GnutellaStatus.initializing

  def getStatus = gnutellaStatus

  def isWaitingPong = { gnutellaStatus == GnutellaStatus.waitingPong }

  def main(args: Array[String]): Unit = {

  }
}

object GnutellaStatus {
  val initializing = 1
  val waitingPong = 2
  val running = 3
  val waitingQueryHits = 4
}
