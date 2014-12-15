package actors

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{SubscribeAck, Publish, Subscribe, Unsubscribe}
import com.fasterxml.jackson.databind.node.{ObjectNode}
import play.api.libs.json.{JsObject, Json}

class PlayerActor(mName: String, out: ActorRef) extends Actor with ActorLogging {
  var mScore: Int = 0

  val mediator = DistributedPubSubExtension(context.system).mediator
  val mTopic = "playersstats"

  override def postStop(): Unit = {
    mediator ! Unsubscribe(mTopic, self)
    mediator ! Publish(mTopic, GameProtocol.Quit(mName))
    println(s"$mName left the game")
  }

  // client handling
  def receive: Actor.Receive = {
    //First step after actor creation
    case GameProtocol.Init() =>
      println(s"$mName joined snake")
      mediator ! Subscribe(mTopic, self)

    //after subscribe, ask all existing PlayerActor to send me current state
    case SubscribeAck(Subscribe(topic, None, `self`)) =>
      println(s"$mName now receives updates")
      mediator ! Publish(topic, GameProtocol.GetState)

    //Set new score of this actor
    case GameProtocol.Set(score) =>
      println(s"$mName: sets score to $score")
      mScore = score
      mediator ! Publish(mTopic, GameProtocol.Update(mName, score))

    //Update of another PlayerActor
    case GameProtocol.Update(playerName, score) =>
      if (playerName != mName) {
        println(s"$mName: update received, new score of $playerName is $score")
        val message = GameProtocol.createUpdateMessage(playerName, score)
        out ! message.toString()
      }

    //Another actor asks for my state
    case GameProtocol.GetState =>
      println(s"$mName: Another actor wants my score")
      sender() ! GameProtocol.Update(mName, mScore)

    //Another PlayerActor quit the game
    case GameProtocol.Quit(playerName) =>
      println(s"$mName: $playerName quits.")
      if (playerName != mName) {
        val message = GameProtocol.createQuitMessage(playerName)
        out ! message.toString()
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

  case class Init()

  case class GetState()


}