package interpreter

import java.net.InetAddress
import java.nio.{ByteOrder, ByteBuffer}

import akka.actor.ActorContext
import descriptor.QueryHitsDescriptor
import descriptor.QueryHitsDescriptor.Result
import gnutella.{GnutellaStatus, Gnutella}
import util.Logger

import scala.collection.mutable.ListBuffer

/**
 * QueryHitsDescriptorのバイト列を解釈して処理を行なうオブジェクト
 * Created by Junya on 15/05/16.
 */
object QueryHitsInterpreter extends HeaderInterpreter {

  def execute(header: Array[Byte], payload: Array[Byte],
              callerContext: ActorContext): Option[QueryHitsDescriptor] = {
    val queryHits = parse(header, payload)

    if (Gnutella.getStatus == GnutellaStatus.waitingQueryHits) {
      queryHits.resultSet.foreach(r =>
        Logger.info("QueryHits! -> " + r.sharedFileName)
      )
    }
    Option(queryHits)
  }

  private def parse(header: Array[Byte], payload: Array[Byte]): QueryHitsDescriptor = {
    val queryHits = new QueryHitsDescriptor
    parseHeader(header, queryHits)

    //payload
    queryHits.numberOfHits = payload(0)
    queryHits.port = ByteBuffer.allocate(2).put(payload.slice(1, 3))
      .order(ByteOrder.LITTLE_ENDIAN).getShort(0)
    queryHits.ipAddress = InetAddress.getByAddress(payload.slice(3, 7))

    val speedField = ByteBuffer.allocate(4).put(payload.slice(7, 11))
      .order(ByteOrder.LITTLE_ENDIAN).getInt(0)
    parseSpeedField(speedField, queryHits)

    parseResultSet(payload, queryHits)

    // OptionalQHDはOptionalなので無いはず

    queryHits.serventIdentifier = new String(ByteBuffer.allocate(16)
      .put(payload.slice(payload.length - 16, payload.length)).order(ByteOrder.LITTLE_ENDIAN).array())

    queryHits
  }

  private def parseSpeedField(speedField: Int, queryHits: QueryHitsDescriptor): Unit = {
    if ((speedField & QueryHitsDescriptor.extendBitMask) != 0) {
      queryHits.firewalledIndicator
        = (speedField & QueryHitsDescriptor.firewallBitMask) != 0
      queryHits.xmlMetaData
        = (speedField & QueryHitsDescriptor.xmlMetadataBitMask) != 0
    } else {
      queryHits.speed = speedField
    }
  }

  /**
   * Result set のパースを行なう
   * @param payload
   * @param queryHits
   */
  private def parseResultSet(payload: Array[Byte], queryHits: QueryHitsDescriptor): Unit = {
    //Optional QHDは無いと仮定
    val rsBytes = payload.slice(QueryHitsDescriptor.resultSetOffset, payload.length - 16)
    val resultSet = ListBuffer[Result]()
    var offset = 0
    while (offset < rsBytes.length) {
      parseResult(rsBytes.slice(offset, rsBytes.length)) match {
        case Some(r) => resultSet += r; offset += r.getByteSize
        case None => offset = Int.MaxValue
      }
    }
    queryHits.resultSet_(resultSet.toArray)
  }

  /**
   * ResultSetフィールドの未パースな部分のByte列を受け取って，Resultに変換する
   * @param bytesHead
   * @return
   */
  private def parseResult(bytesHead: Array[Byte]): Option[Result] = {
    if (bytesHead.length <= 10) {
      return None
    }
    val fileIndex = ByteBuffer.allocate(4).put(bytesHead.slice(0, 4))
      .order(ByteOrder.LITTLE_ENDIAN).getInt(0)
    val fileSize = ByteBuffer.allocate(4).put(bytesHead.slice(4, 8))
      .order(ByteOrder.LITTLE_ENDIAN).getInt(0)
    val fileName = new String(
      bytesHead.slice(8, bytesHead.length).takeWhile(_ != 0.toByte).reverse
    )
    val option = new String(
      // 8 + file名の長さ + Null文字分1Byte
      bytesHead.slice(8 + fileName.getBytes.length + 1, bytesHead.length)
        .takeWhile(_ != 0.toByte)
    )
    Option(new Result(fileIndex, fileSize, fileName, option))
  }

}
