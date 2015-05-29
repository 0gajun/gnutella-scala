package gnutella

import java.net.InetAddress

import descriptor.QueryHitsDescriptor.Result

import scala.collection.mutable.ListBuffer

/**
 * Created by Junya on 15/05/29.
 */
object ResultSetsPreserver {

  private val resultSets = ListBuffer.empty[QueryResultInfo]

  def initialize(): Unit = ResultSetsPreserver.synchronized {
    resultSets.clear()
  }

  def addResult(result: Result, ip:InetAddress, port: Int): Unit = ResultSetsPreserver.synchronized {
    resultSets+=QueryResultInfo(result, ip, port)
  }

  def getResults: Seq[QueryResultInfo] = ResultSetsPreserver.synchronized {
    resultSets.toList
  }

}

case class QueryResultInfo(result: Result, ip: InetAddress, port: Int)
