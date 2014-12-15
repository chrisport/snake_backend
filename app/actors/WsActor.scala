package actors

import akka.actor._
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.Play.current

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
      println(msg)
      val jsonNode = mapper.readTree(msg)
      val cmd: String = jsonNode.get("cmd").asText
      if (playerActor == null) {
        System.out.println("actor was null")
        if (cmd == "enter") {
          System.out.println("command is \"enter\"")
          playerName = jsonNode.get("data").get("name").textValue()
          playerActor = Akka.system.actorOf(Props(classOf[PlayerActor], playerName, out))
          System.out.println(s"playerActor $playerName created")
          val initMessage: GameProtocol.Init = new GameProtocol.Init()

          playerActor ! initMessage
          System.out.println("initialized playerActor")
        } else {
          val error = Json.obj(
            "cmd" -> "error",
            "data" -> Json.obj(
              "message" -> "First command must be enter"
            )
          )
          out ! error.toString
        }
      }
      else if (cmd == "update") {
        val score: Int = jsonNode.get("data").get("score").asInt
        val message: GameProtocol.Set = new GameProtocol.Set(score)
        playerActor ! message
      }
  }

}