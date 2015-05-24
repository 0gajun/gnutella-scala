package descriptor

import java.net.{InetAddress, Inet4Address}
import java.nio.ByteBuffer

import descriptor.QueryHitsDescriptor.Result

import scala.collection.mutable

class QueryHitsDescriptor extends DescriptorHeader {
  override protected var payloadDescriptor: Int = DescriptorHeader.P_DESC_QUERY_HITS

  //----------------------------------------------------------------------------------
  //|Number of Hits|Port    |IP Address|Speed  |Result Set |Optional QHD |servent id |
  //| 1byte        | 2byte  | 4byte    | 4byte | xxxbyte   | yyybyte     | 16byte    |
  //----------------------------------------------------------------------------------
  // 1 + 2 + 4 + 4 + xxx + yyy + 16
  override def payloadLength: Int = 27 +
    _resultSet.foldLeft(0)((accum, res: Result) => accum + res.getByteSize) +
    optionalQhdData.size

  var numberOfHits: Byte = _
  var port: Short = _
  var ipAddress: InetAddress = _

  @Deprecated
  var speed: Int = -1
  var firewalledIndicator: Boolean = _
  var xmlMetaData: Boolean = _

  private var _resultSet: Array[Result] = Array()
  var optionalQhdData: Array[Byte] = Array()
  var serventIdentifier: String = _

  def resultSet: Array[Result] = _resultSet

  def resultSet_(data: Array[Result]): Unit = {
    _resultSet = data
  }

  override def toByteArray(): Array[Byte] = {
    val header = convertHeaderToByteArray()
    val numberOfHitsByte = Array(numberOfHits)
    val portByte = ByteBuffer.allocate(2).putShort(port).array.reverse
    val ipByte = ipAddress.getAddress
    val speedByte = getSpeedByte
    val idByte = serventIdentifier.getBytes.reverse
    val resultByte = getResultSetByte
    val optionByte = optionalQhdData.reverse

    Array.concat(header, numberOfHitsByte,portByte, ipByte, speedByte, resultByte, optionByte, idByte)
  }

  private def getResultSetByte: Array[Byte] = {
    val builder = mutable.ArrayBuilder.make[Byte]()
    builder.sizeHint(resultSet.map(_.byteArray.size).sum)
    resultSet.foreach(builder ++= _.byteArray)
    builder.result()
  }

  private def getSpeedByte: Array[Byte] = {
    if (speed != -1) {
      var extend = QueryHitsDescriptor.extendBitMask
      if (firewalledIndicator)
        extend |= QueryHitsDescriptor.firewallBitMask
      if (xmlMetaData)
        extend |= QueryHitsDescriptor.xmlMetadataBitMask
      ByteBuffer.allocate(4).putInt(extend).array.reverse
    } else {
      val legacy: Int = speed & 0x01111111 // Legacy format
      ByteBuffer.allocate(4).putInt(legacy).array.reverse
    }
  }
}

object QueryHitsDescriptor {

  val extendBitMask: Int = 0x10000000
  val firewallBitMask: Int = 0x01000000
  val xmlMetadataBitMask: Int = 0x00100000

  val resultSetOffset = 11
  val serventIdLength = 16

  //----------------------------------------------------------------------------------
  //| File Index | File Size | Shared File Name | Null  | Optional Result Data | Null |
  //| 4byte      | 4byte     | xxxbyte          | 1byte | yyybyte              | 1byte|
  //----------------------------------------------------------------------------------
  class Result(val fileIndex: Int, val fileSize: Int, val sharedFileName: String, val optionalResultData: String) {
    private val indexByte = ByteBuffer.allocate(4).putInt(fileIndex).array.reverse
    private val sizeByte = ByteBuffer.allocate(4).putInt(fileSize).array.reverse
    private val nameByte = sharedFileName.getBytes.reverse
    private val optionByte = optionalResultData.getBytes.reverse
    private val nullByte = Array(0.toByte)
    val byteArray = Array.concat(indexByte, sizeByte, nameByte, nullByte, optionByte, nullByte)

    def getByteSize = byteArray.length
  }

}
