package actors

import akka.actor._
import commands.ClientCommand
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.Play.current

object WsActor {
  def props(out: ActorRef) = Props(new WsActor(out))
}

class WsActor(out: ActorRef) extends Actor with ActorLogging {
  var playerName: String = null
  val playerActor: ActorRef = Akka.system.actorOf(Props(classOf[PlayerActor], out))

  def receive = {
    case msg: String =>
      log.info("Incoming message: " + msg)
      val json = Json.parse(msg)
      json.validate(ClientCommand.reader).map {

        case initMessage @ GameProtocol.Init(playerName) =>
          this.playerName = playerName
          playerActor ! initMessage
          log.info(s"$playerName enter the game")

        case updateMessage @ GameProtocol.Set(score) =>
          playerActor ! updateMessage
          log.info(s"$playerName: set score to $score")

        case cmd =>
          val error = Json.obj(
            "cmd" -> "error",
            "data" -> Json.obj(
              "message" -> s"Invalid cmd: $cmd"
            )
          )
          out ! error.toString
      }.recoverTotal {
        case _ =>
          val error = Json.obj(
            "cmd" -> "error",
            "data" -> Json.obj(
              "message" -> s"Missing cmd"
            )
          )
          out ! error.toString
      }
  }
}