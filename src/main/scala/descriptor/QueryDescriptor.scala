package descriptor

import java.nio.ByteBuffer

class QueryDescriptor extends DescriptorHeader {
  override protected var payloadDescriptor: Int = DescriptorHeader.P_DESC_QUERY

  var minimumSpeed: Short = _
  var searchCriteria: String = _
  var queryData: Array[Byte] = Array()

  //----------------------------------------------------
  //min speed|search criteria|Null terminate|Query data|
  // 2byte   | xxx byte      | 1 byte       | yyy byte |
  //----------------------------------------------------
  // length: 2 + xxx + 1 + yyy
  override def payloadLength: Int = 2 + searchCriteria.length + 1 + queryData.length

  override def toByteArray(): Array[Byte] = {
    val header = convertHeaderToByteArray()
    val speedByte = ByteBuffer.allocate(2).putShort(minimumSpeed).array.reverse
    val criteriaByte = searchCriteria.getBytes("UTF-8")
    val nullByte = Array(0.toByte)

    Array.concat(header, speedByte, criteriaByte, nullByte, queryData.reverse)
  }
}
