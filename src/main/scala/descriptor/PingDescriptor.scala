package descriptor

class PingDescriptor extends DescriptorHeader{

  override protected var payloadDescriptor: Int = DescriptorHeader.P_DESC_PING
  var optionalPingData: Array[Byte] = Array()

  override def toByteArray(): Array[Byte] = {
    val header = super.toByteArray()
    header++optionalPingData.reverse
  }

  override def payloadLength:Int= optionalPingData.size
}
