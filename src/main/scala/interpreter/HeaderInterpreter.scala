package interpreter

import java.nio.{ByteOrder, ByteBuffer}

import descriptor.{PingDescriptor, DescriptorHeader}
import org.apache.commons.codec.binary.Hex

/**
 * Created by Junya on 15/05/07.
 */
abstract class HeaderInterpreter {

  /**
   * headerのバイト列を解釈して，引数のdescに格納する
   * TTL及びHopsの操作も含む
   *
   * @param header
   * @param desc
   */
  def parseHeader(header: Array[Byte], desc: DescriptorHeader): Unit = {
    desc.descriptorId(Hex.encodeHexString(header.slice(0, 16).reverse))
    //Hopsの増加及びTTLの減少
    desc.ttl = header(17).toInt - 1
    desc.hops = header(18).toInt + 1
    // PayLoadLengthは自動的に計算
    // PayloadDescriptorはクラス生成時に自動的に設定されているのでここでは設定しない
  }

}
