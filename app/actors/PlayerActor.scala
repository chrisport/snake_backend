package actors

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{SubscribeAck, Publish, Subscribe, Unsubscribe}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ObjectNode}
import play.libs.Json
import play.mvc.WebSocket

class PlayerActor(mName: String, out: WebSocket.Out[JsonNode]) extends Actor with ActorLogging {
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
    case GameProtocol.Init =>
      println(s"$mName joined snake")
      mediator ! Subscribe(mTopic, self)

    //after subscribe, ask all existing PlayerActor to send me current state
    case SubscribeAck(Subscribe(topic, None, `self`)) =>
      println(s"$mName now receives updates")
      mediator ! Publish(topic, GameProtocol.GetState)

    //Set new score of this actor
    case GameProtocol.Set(score) =>
      mScore = score
      mediator ! Publish(mTopic, GameProtocol.Update(mName, score))

    //Update of another PlayerActor
    case GameProtocol.Update(playerName, score) =>
      val message = GameProtocol.createUpdateMessage(playerName, score)
      out.write(message)

    //Another actor asks for my state
    case GameProtocol.GetState =>
      sender() ! GameProtocol.Update(mName, mScore)

    //Another PlayerActor quit the game
    case GameProtocol.Quit(playerName) =>
      if (playerName != mName) {
        val message = GameProtocol.createQuitMessage(playerName)
        out.write(message)
      }
  }
}


object GameProtocol {

  case class Set(score: Int)

  case class Update(playerName: String, score: Int)

  def createUpdateMessage(playerName: String, score: Int): ObjectNode = {
    val data: ObjectNode = Json.newObject
    data.put("name", playerName)
    data.put("score", score)
    data.put("event", "set")

    val message: ObjectNode = Json.newObject
    message.put("data", data)
    message.put("cmd", "quit")
    message
  }

  case class Quit(name: String)

  def createQuitMessage(playerName: String): ObjectNode = {
    val data: ObjectNode = Json.newObject
    data.put("name", playerName)
    data.put("event", "quit")

    val message: ObjectNode = Json.newObject
    message.put("data", data)
    message.put("cmd", "quit")
    message
  }

  case class Init()

  case class GetState()


}