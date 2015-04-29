package descriptor

import java.nio.ByteBuffer

import org.apache.commons.codec.binary.Hex

class DescriptorHeader() {
  private val headerSize = 23

  var descriptorId: String = _
  var payloadDescriptor: Int = _
  // unsigned
  var ttl: Int = _
  // unsigned
  var hops: Int = _
  var payloadLength: Int = _

  // little endian
  def convertHeaderToByteArray(): Array[Byte] = {
    val array: Array[Byte] = new Array[Byte](headerSize)
    var offset = headerSize - 1

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

  def toByteArray() = {
    convertHeaderToByteArray()
  }

}

object DescriptorHeader {
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
}