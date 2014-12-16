package commands

import actors.GameProtocol
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by chrisport on 16/12/14.
 */
object ClientCommand {
  val cmdReads = (__ \ 'cmd).read[String].map(_.toLowerCase)
  val dataReads = (__ \ 'data).read[JsObject] orElse Reads.pure(Json.obj())

  def reader = (cmdReads and dataReads)(
    (cmd, body) => cmd match {

      case "enter" if (body \ "name").asOpt[String].isDefined =>
        GameProtocol.Init((body \ "name").asOpt[String].get)
      case "update" if (body \ "score").asOpt[Int].isDefined=>
          GameProtocol.Set((body \ "score").asOpt[Int].get)
      case _ => s"malformed request: $cmd, $body"
    }
  )
}
