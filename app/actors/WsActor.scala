package actors

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{SubscribeAck, Publish, Subscribe, Unsubscribe}
import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import com.fasterxml.jackson.databind.node.{ObjectNode}
import play.libs.Json
import play.mvc.WebSocket

object WsActor {
  def props(out: ActorRef) = Props(new WsActor(out))
}

class WsActor(out: ActorRef) extends Actor {
  var playerName: String = null
  var playerActor: ActorRef = null
  val mapper = new ObjectMapper()
  mapper.readTree("{\"k1\":\"v1\"}")

  def receive = {
    case msg: String =>
      val jsonNode = mapper.readTree(msg)
      val cmd: String = jsonNode.get("cmd").asText
      if (playerActor == null) {
        System.out.println("actor was null")
        if (cmd == "enter") {
          System.out.println("command is \"enter\"")
          val player: PlayerActor = new PlayerActor(playerName, out)
          System.out.println("playerActor created")
          val initMessage: GameProtocol.Init = new GameProtocol.Init
          System.out.println("initialized playerActor")
        }
        else {
          val errorData: ObjectNode = Json.newObject
          errorData.put("message", "First command must be enter")
          val error: ObjectNode = Json.newObject
          error.put("cmd", "error")
          error.put("data", errorData)
          out ! error.toString
        }
      }
      else if (cmd == "update") {
        val score: Int = jsonNode.get("data").get("score").asInt
        val message: GameProtocol.Set = new GameProtocol.Set(score)
        playerActor.tell(message, null)
      }
  }

}