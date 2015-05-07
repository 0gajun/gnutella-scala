package descriptor

import java.nio.{ByteOrder, ByteBuffer}
import java.util.UUID

import org.apache.commons.codec.binary.Hex
import util.Logger

abstract class DescriptorHeader() {

  //-----------------------------------------------------------------
  // Descriptor Id |Payload Descriptor|TTL   |Hops  |Payload Length |
  // 16Byte        | 1Byte            |1Byte |1Byte | 4Byte         |
  //-----------------------------------------------------------------

  private[this] var _descriptorId: String = UUID.randomUUID().toString.replace("-", "")
  protected var payloadDescriptor: Int
  // unsigned
  var ttl: Int = _
  // unsigned
  var hops: Int = _

  // フィールドには存在するが，抽象メソッドにより子クラスで計算させる
  // var payloadLength: Int = _
  def payloadLength: Int

  def descriptorId = _descriptorId
  def descriptorId(id: String) = { _descriptorId = id }

  def convertHeaderToByteArray(): Array[Byte] = {
    val array: Array[Byte] = new Array[Byte](DescriptorHeader.headerSize)
    var offset = DescriptorHeader.headerSize - 1

    // リトルエンディアンで格納する
    ByteBuffer.allocate(4).putInt(payloadLength).array.foreach( b => {
      array(offset) = b
      offset -= 1
    })
    array(offset) = hops.toByte
    array(offset-1) = ttl.toByte
    array(offset-2) = payloadDescriptor.toByte
    offset -= 3

    Hex.decodeHex(descriptorId.toCharArray).foreach(b => {
      array(offset) = b
      offset -= 1
    })

    array
  }

  def toByteArray(): Array[Byte]
}

object DescriptorHeader {
  val headerSize = 23
  val payloadDescriptorOffset = 16
  //Payload Descriptors
  val P_DESC_PING = 0x00
  val P_DESC_PONG = 0x01
  val P_DESC_QUERY = 0x80
  val P_DESC_QUERY_HITS = 0x81
  val P_DESC_PUSH = 0x40
  // Extension Descriptors
  val P_DESC_BYE = 0x02
  val P_DESC_IBCM = 0x10
  val P_DESC_QRP = 0x30
  val P_DESC_OPEN_VECTOR_EXTENSION = 0x31
  val P_DESC_STANDARD_VENDOR_EXTENSION = 0x32

  def calcPayloadLength(headerByte: Array[Byte]): Int = {
    if (headerByte.length != headerSize) {
      Logger.info("header size is incorrect")
      return -1
    }
    ByteBuffer.allocate(4).put(headerByte, 19, 4).order(ByteOrder.LITTLE_ENDIAN).getInt(0)
  }

}