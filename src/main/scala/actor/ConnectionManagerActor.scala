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
    case RunConnectionActor(s) => runActor(_)
    case BroadcastMessage(c, m) => broadcastMessage(c, m)
    case ForwardMessage(m) => forwardMessage(m)
  }

  /**
   * 引数のsocketを持つActorをスタートさせる
   * @param socket
   */
  private def runActor(socket: Socket): Unit = {
    val actor = context.actorOf(Props[ConnectionActor], socket.getInetAddress.toString + socket.getPort)
    context watch actor
    actor ! RunConnectionActor(socket)
    actor ! "run"
  }

  /**
   * メッセージを受け取ったコネクション以外全てに転送する
   * @param caller
   * @param message
   */
  private def broadcastMessage(caller: ConnectionActor, message: DescriptorHeader): Unit = {
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
   * メッセージをルーティング表に則って転送する．
   *
   * @param message
   */
  private def forwardMessage(message: DescriptorHeader): Unit = {
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

case class RunConnectionActor(socket: Socket)
case class BroadcastMessage(caller: ConnectionActor, message: DescriptorHeader)
case class ForwardMessage(message: DescriptorHeader)
