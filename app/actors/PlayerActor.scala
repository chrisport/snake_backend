package actors

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{SubscribeAck, Publish, Subscribe, Unsubscribe}
import com.fasterxml.jackson.databind.node.{ObjectNode}
import play.api.libs.json.{JsObject, Json}

class PlayerActor(out: ActorRef) extends Actor with ActorLogging {
  var mScore: Int = 0

  val mediator = DistributedPubSubExtension(context.system).mediator
  val mTopic = "playersstats"
  var mName = "unnamed"

  override def postStop(): Unit = {
    mediator ! Unsubscribe(mTopic, self)
    mediator ! Publish(mTopic, GameProtocol.Quit(mName))
    println(s"$mName left the game")
  }

  // initial state
  def receive: Actor.Receive = {
    //First step after actor creation
    case GameProtocol.Init(playerName) =>
      mName = playerName
      println(s"$mName joined snake")
      context become playing
      mediator ! Subscribe(mTopic, self)
    case _ =>
      val error = Json.obj(
        "cmd" -> "error",
        "data" -> Json.obj(
          "message" -> "First command must be enter"
        )
      )
      out ! error.toString
  }

  def playing: Receive = receivePubsubUpdates orElse receiveClientMessage orElse {
    case msg =>
      //log.error(s"unknown message $msg")
  }

  // playing state
  def receivePubsubUpdates: Actor.Receive = {
    //after subscribe, ask all existing PlayerActor to send me current state
    case SubscribeAck(Subscribe(topic, None, `self`)) =>
      log.info(s"$mName now receives updates")
      mediator ! Publish(topic, GameProtocol.GetState(mName))

    //Update of another PlayerActor
    case GameProtocol.Update(playerName, score) if playerName != mName =>
      log.info(s"$mName: update received, new score of $playerName is $score")
      val message = GameProtocol.createUpdateMessage(playerName, score)
      out ! message.toString()

    //Another PlayerActor quit the game
    case GameProtocol.Quit(playerName) if playerName != mName =>
      log.info(s"$mName: $playerName quits.")
      val message = GameProtocol.createQuitMessage(playerName)
      out ! message.toString()
  }

  def receiveClientMessage: Actor.Receive = {
    //Set new score of this actor
    case GameProtocol.Set(score) if score == 0 || score > mScore =>
      log.info(s"$mName: sets score to $score")
      mScore = score
      mediator ! Publish(mTopic, GameProtocol.Update(mName, score))

    //Another actor asks for my state
    case GameProtocol.GetState(playerName) =>
      if (playerName != mName) {
        log.info(s"$mName: Another actor wants my score")
        sender() ! GameProtocol.Update(mName, mScore)
      }
  }

}

object GameProtocol {

  case class Set(score: Int)

  case class Update(playerName: String, score: Int)

  def createUpdateMessage(playerName: String, score: Int): JsObject = {
    Json.obj(
      "cmd" -> "update",
      "data" -> Json.obj(
        "name" -> playerName,
        "score" -> score,
        "event" -> "set"
      )
    )
  }

  case class Quit(name: String)

  def createQuitMessage(playerName: String): JsObject = {
    Json.obj(
      "cmd" -> "update",
      "data" -> Json.obj(
        "name" -> playerName,
        "event" -> "quit"
      )
    )
  }

  case class Init(playerName: String)

  case class GetState(playerName: String)


}