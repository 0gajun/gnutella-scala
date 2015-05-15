package descriptor

import java.net.{InetAddress, Inet4Address}
import java.nio.ByteBuffer

import descriptor.QueryHitsDescriptor.ResultSet

import scala.collection.mutable

class QueryHitsDescriptor extends DescriptorHeader {
  override protected var payloadDescriptor: Int = DescriptorHeader.P_DESC_QUERY_HITS

  //----------------------------------------------------------------------------------
  //|Number of Hits|Port    |IP Address|Speed  |Result Set |Optional QHD |servent id |
  //| 1byte        | 2byte  | 4byte    | 4byte | xxxbyte   | yyybyte     | 16byte    |
  //----------------------------------------------------------------------------------
  // 1 + 2 + 4 + 4 + xxx + yyy + 16
  override def payloadLength: Int = 23+resultSet.size+optionalQhdData.size

  var numberOfHits: Byte = _
  var port: Short = _
  var ipAddress: InetAddress = _

  @Deprecated
  var speed: Short = -1
  var firewalledIndicator: Boolean = _
  var xmlMetaData: Boolean = _

  private var resultSet: Array[Byte] = Array()
  var optionalQhdData: Array[Byte] = Array()
  var serventIdentifier: String = _

  def setResultSets(data: Array[ResultSet]): Unit = {
    val builder = mutable.ArrayBuilder.make[Byte]()
    builder.sizeHint(data.map(_.byteArray.size).sum)
    data.foreach(builder++=_.byteArray)
    resultSet = builder.result()
  }

  override def toByteArray(): Array[Byte] = {
    val header = convertHeaderToByteArray()
    val portByte = ByteBuffer.allocate(2).putShort(port).array.reverse
    val ipByte = ipAddress.getAddress
    val speedByte = getSpeedByte
    val idByte = serventIdentifier.getBytes.reverse
    val optionByte = optionalQhdData.reverse

    Array.concat(header, portByte, ipByte, speedByte, resultSet, optionByte, idByte)
  }

  private def getSpeedByte: Array[Byte] = {
    if (speed != -1) {
      var extend = 0x10000000
      if (firewalledIndicator)
        extend|=0x01000000
      if (xmlMetaData)
        extend|=0x00100000
      ByteBuffer.allocate(4).putInt(extend).array.reverse
    } else {
      val legacy: Int = speed&0x01111111// Legacy format
      ByteBuffer.allocate(2).putInt(legacy).array.reverse
    }
  }
}

object QueryHitsDescriptor {

  //----------------------------------------------------------------------------------
  //| File Index | File Size | Shared File Name | Null  | Optional Result Data | Null |
  //| 4byte      | 4byte     | xxxbyte          | 1byte | yyybyte              | 1byte|
  //----------------------------------------------------------------------------------
  class ResultSet(fileIndex: Int, fileSize: Int, sharedFileName: String, optionalResultData: String) {
    private val indexByte = ByteBuffer.allocate(4).putInt(fileIndex).array.reverse
    private val sizeByte = ByteBuffer.allocate(4).putInt(fileSize).array.reverse
    private val nameByte = sharedFileName.getBytes.reverse
    private val optionByte = optionalResultData.getBytes.reverse
    private val nullByte = Array(0.toByte)
    val byteArray = Array.concat(indexByte, sizeByte, nameByte, nullByte, optionByte, nullByte)

    def getByteSize = byteArray.length
  }
}
