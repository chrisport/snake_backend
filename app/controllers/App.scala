package controllers

import actors.WsActor
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.{Json, JsValue, JsObject}
import play.api.mvc.{WebSocket, Controller}

object App extends Controller {
  import play.api.mvc._
  import play.api.Play.current

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    WsActor.props(out)
  }
}
