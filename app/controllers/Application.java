package controllers;

import actors.*;
import akka.actor.*;
import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

/**
 * The main web controller that handles returning the index page, setting up a WebSocket, and watching a stock.
 */
public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render());
    }

    public static WebSocket<JsonNode> ws() {
        return new WebSocket<JsonNode>() {
            String playerName = null;
            ActorRef playerActor = null;

            public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
                System.out.println("ready");
                // send all WebSocket message to the UserActor
                in.onMessage(jsonNode -> {
                    String cmd = jsonNode.get("cmd").asText();
                    if (playerActor == null) {
                        System.out.println("actor was null");

                        if (cmd.equals("enter")) {
                            System.out.println("command is \"enter\"");
                            playerActor = Akka.system().actorOf(Props.create(PlayerActor.class, out, playerName));
                            System.out.println("playerActor created");

                            GameProtocol.Init initMessage = new GameProtocol.Init();
                            playerActor.tell(initMessage, null);
                            System.out.println("initialized playerActor");

                        } else {
                            ObjectNode errorData = Json.newObject();
                            errorData.put("message", "First command must be enter");

                            ObjectNode error = Json.newObject();
                            error.put("cmd", "error");
                            error.put("data", errorData);
                            out.write(error);
                        }
                    } else if (cmd.equals("update")) {
                        int score = jsonNode.get("data").get("score").asInt();
                        GameProtocol.Set message = new GameProtocol.Set(score);
                        playerActor.tell(message, null);
                    }
                });

                // on close, tell the userActor to shutdown
                in.onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        Akka.system().stop(playerActor);
                    }
                });
            }
        };
    }

}
