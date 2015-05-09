package actor

import java.net.Socket

import akka.actor._
import descriptor.DescriptorHeader
import util.Logger

import scala.collection.mutable

/**
 * ConnectionActorを管理するアクター
 * メッセージのルーティングとActorの管理を行う
 *
 * Created by Junya on 15/05/01.
 */
class ConnectionManagerActor extends Actor {
  /**
   * メッセージのルーティングを行うためのテーブル.
   * Ping及びQueryのDescriptorIDを保存し，Pong及びQueryHitsを適切なActorに振り分けられるようにする
   * [DescriptorId, Actor's name]で格納
   */
  private[this] val routingTable = new mutable.OpenHashMap[String, String]()

  //TODO: ルーティングテーブルの肥大化を防ぐ処理実装

  /**
   * レシーブ
   * @return
   */
  override def receive: Receive = {
    case RunConnectionActor(s) => runActor(s)
    case BroadcastMessage(c, m) => broadcastMessage(c, m)
    case ForwardMessage(m) => forwardMessage(m)
    case SendMessageToAllConnections(m) => sendMessageToAllConnections(m)
  }

  /**
   * 引数のsocketを持つActorをスタートさせる
   * @param socket
   */
  private def runActor(socket: Socket): Unit = {
    val actor = context.actorOf(Props[ConnectionActor],
      "connection" + socket.getInetAddress.getHostAddress + socket.getPort)
    actor ! CreateConnectionActor(socket)
    actor ! "run"
    Logger.debug("connection created")
  }

  /**
   * メッセージを受け取ったコネクション以外全てに転送する
   * @param caller
   * @param message
   */
  private def broadcastMessage(caller: ConnectionActor, message: DescriptorHeader): Unit = {
    Logger.debug("broadcastMessage")
    // Forward済みのDescriptorはBroadcastしない
    if (routingTable.contains(message.descriptorId)) {
      Logger.info("Message is already forwarded. Discarded.")
      return
    }

    // ルーティングテーブルに追加
    addRouting(message.descriptorId, caller.self.path.name)

    // ブロードキャスト
    context.children.filter(_.path.name != caller.self.path.name)
      .foreach(_ ! SendMessage(message))
  }

  /**
   * ConnectionManagerの保持する全コネクションに対してメッセージを送信する
   * @param message 送信したいメッセージ
   */
  private def sendMessageToAllConnections(message: DescriptorHeader): Unit = {
    context.children.foreach(_ ! SendMessage(message))
  }

  /**
   * メッセージをルーティング表に則って転送する．
   *
   * @param message
   */
  private def forwardMessage(message: DescriptorHeader): Unit = {
    Logger.debug("forwardMessage")
    routingTable.get(message.descriptorId) match {
      case Some(s) =>
        val actor = context.actorSelection(s)
        // メッセージ配送
        actor ! SendMessage(message)
      case None =>
        Logger.info("Message's descriptor id doesn't exist in routing table. This message was discarded")
    }
  }

  /**
   * ルーティングエントリの追加
   * @param descriptorId
   * @param actorName
   */
  private def addRouting(descriptorId: String,actorName: String): Unit = {
    routingTable+=(descriptorId->actorName)
  }
}

object ConnectionManagerActor {
  val name = "connectionManager"
}

case class RunConnectionActor(socket: Socket)
case class BroadcastMessage(caller: ConnectionActor, message: DescriptorHeader)
case class ForwardMessage(message: DescriptorHeader)
case class SendMessageToAllConnections(message: DescriptorHeader)
