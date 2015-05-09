package interpreter

import java.net.InetAddress

import actor.SendMessage
import akka.actor.{ActorContext, ActorRef}
import descriptor.{PingDescriptor, PongDescriptor}
import util.Logger

/**
 * PingDescriptorのバイト列を解釈して処理を行うオブジェクト
 * Created by Junya on 15/05/03.
 */
object PingInterpreter extends HeaderInterpreter {

  /**
   * PingDescriptorに対する処理を行う
   * @param header ヘッダーのバイト列
   * @param payload ペイロードのバイト列
   * @param callerContext 呼び出し元のActorRef
   * @return 転送するDescriptor
   */
  def execute(header: Array[Byte], payload: Array[Byte], callerContext: ActorContext): Option[PingDescriptor] = {
    val ping = parse(header, payload)
    Logger.info("ping: " + ping.descriptorId)
    pongReply(ping, callerContext.self)
    Option(ping)
  }

  /**
   * Pingに対するPong応答を送信するためのメッセージをActorへ送る
   * @param ping 受信したPing
   * @param caller 呼び出し元のActorRef
   */
  private def pongReply(ping: PingDescriptor, caller: ActorRef): Unit = {
    val pong = new PongDescriptor
    // Header
    pong.descriptorId(ping.descriptorId)
    pong.ttl = ping.hops
    pong.hops = 0
    // Payload
    pong.port = 6346//TODO: PORT番号をどうするか
    pong.ipAddress = InetAddress.getLocalHost
    pong.numberOfFilesShared = 100 //TODO: ファイル管理機構実装後，修正
    pong.numberOfKilobytesShared = 100 //TODO: ファイル管理機構実装後，修正
    pong.optionalPongData = Array() // Option は使わないので空

    caller ! SendMessage(pong)
  }

  /**
   * バイト列をオブジェクトに変換する
   * @param header ヘッダーのバイト列
   * @param payload ペイロードのバイト列
   * @return オブジェクト
   */
  private def parse(header: Array[Byte], payload: Array[Byte]): PingDescriptor = {
    val ping = new PingDescriptor
    ping.optionalPingData = payload.reverse
    parseHeader(header, ping)
    ping
  }
}
