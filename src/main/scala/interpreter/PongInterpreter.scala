package interpreter

import java.net.InetAddress
import java.nio.{ByteOrder, ByteBuffer}

import actor.ServentsManagerActor
import actor.ServentsManagerActor.RegisterServent
import akka.actor.ActorContext
import descriptor.PongDescriptor
import gnutella.Gnutella
import model.ServentInfo
import util.Logger


/**
 * PongDescriptorのバイト列を解釈して処理するオブジェクト
 * Created by Junya on 15/05/07.
 */
object PongInterpreter extends HeaderInterpreter {

  /**
   * PongDescriptorに対する処理を行う
   * @param header ヘッダーのバイト列
   * @param payload ペイロードのバイト列
   * @param callerContext 呼び出し元のActorContext
   * @return 転送するDescriptor
   */
  def execute(header: Array[Byte], payload: Array[Byte], callerContext: ActorContext): Option[PongDescriptor] = {
    val pong = parse(header, payload)
    // 対応するPingを受け取っているか否かは，ConnectionManagerのルーティングテーブルにて解決

    // Pingに対するPong応答を待っていれば，フォワードせずに登録する
    if (Gnutella.isWaitingPong) {
      registerServent(pong, callerContext)
      None
    } else {
      Option(pong)
    }
  }

  /**
   * Pong応答を受信したServentの情報を登録する
   * @param pong 該当PongDescriptor
   * @param callerContext 呼び出し元のActorContext
   */
  private def registerServent(pong: PongDescriptor, callerContext: ActorContext): Unit = {
    val servent = new ServentInfo(pong.port, pong.ipAddress, pong.numberOfFilesShared,
      pong.numberOfKilobytesShared, pong.hops)
    val selection = callerContext.system.actorSelection("user/" + ServentsManagerActor.name)

    Logger.debug(selection.toString() + "@registerServent")
    selection ! RegisterServent(servent)
  }

  /**
   * バイト列をオブジェクトに変換する
   * @param header ヘッダーのバイト列
   * @param payload ペイロードのバイト列
   * @return オブジェクト
   */
  private def parse(header: Array[Byte], payload: Array[Byte]): PongDescriptor = {
    val pong = new PongDescriptor
    parseHeader(header, pong)
    pong.port = ByteBuffer.allocate(2)
      .put(payload.slice(0, 2)).order(ByteOrder.LITTLE_ENDIAN).getShort(0)
    pong.ipAddress = InetAddress.getByAddress(payload.slice(2, 6))
    pong.numberOfFilesShared = ByteBuffer.allocate(4)
      .put(payload.slice(6, 10)).order(ByteOrder.LITTLE_ENDIAN).getInt(0)
    pong.numberOfKilobytesShared = ByteBuffer.allocate(4)
      .put(payload.slice(10, 14)).order(ByteOrder.LITTLE_ENDIAN).getInt(0)
    //Optionalは使わないので空
    pong.optionalPongData = Array()
    pong
  }
}
