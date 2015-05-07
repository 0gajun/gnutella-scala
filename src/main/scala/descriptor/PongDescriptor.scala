package descriptor

import java.net.InetAddress
import java.nio.ByteBuffer

class PongDescriptor extends DescriptorHeader {
  override protected var payloadDescriptor: Int = DescriptorHeader.P_DESC_PONG

  //---------------------------------------------------
  //|port  |IP Addr|Num Of File|Num Of kilo|Optional |
  //| 2byte| 4byte | 4byte     | 4byte      | xxx byte|
  //---------------------------------------------------
  //14(=2+4+4+4) + xxx
  override def payloadLength: Int = 14 + optionalPongData.length

  var port: Short = _
  var ipAddress: InetAddress = _
  var numberOfFilesShared: Int = _
  var numberOfKilobytesShared: Int = _
  var optionalPongData: Array[Byte] = Array()

  override def toByteArray(): Array[Byte] = {
    val header = convertHeaderToByteArray()
    val portByte = ByteBuffer.allocate(2).putShort(port).array.reverse
    val numFileByte = ByteBuffer.allocate(4).putInt(numberOfFilesShared).array.reverse
    val numKiloByte = ByteBuffer.allocate(4).putInt(numberOfKilobytesShared).array().reverse
    Array.concat(header, portByte, ipAddress.getAddress, numFileByte,
      numKiloByte, optionalPongData.reverse)
  }

}
