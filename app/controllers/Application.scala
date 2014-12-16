package controllers

import actors.WsActor
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.{Json, JsValue, JsObject}
import play.api.mvc.{WebSocket, Controller}
import play.mvc.Result

object Application extends Controller {
  import play.api.mvc._
  import play.api.Play.current

  def ws = WebSocket.acceptWithActor[String, String] { request => out =>
    WsActor.props(out)
  }
}
