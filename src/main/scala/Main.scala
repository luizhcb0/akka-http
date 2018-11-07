import Main.TestAPIParams
import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import scalaj.http
import spray.json._

import scala.concurrent.Promise
//import play.api.libs.json._
import org.json4s._
import org.json4s.native.JsonMethods._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.io.StdIn
import scalaj.http._


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val testAPIJsonFormat = jsonFormat2(TestAPIParams)
}


object Main extends App with Directives with JsonSupport {

  val handler = new MessageHandler()

  val userFields = "name,email,hometown,id,gender,birthday,picture,family,work,friends.limit(0)"
  val messageFields = "from,id,to,message"
  val token = "AnyStringToken"
  val secret = "08829edb4d005149ba0935c62f78276a"
  val appId = "284511972397990"
  val appPSID = "357600121333928"
  val pageAccessToken = "EAACUOu3AkJMBAGsN5ZBXaIK9LonwZCvCmLnZAMFtZA7xZCoNic2MjfGWORlJsrZAXJOZBU5tJr2dz3GxeHPpkzAUtWmFClcQkGsKWZCZAJaSVPUBu8WIiiqS25x8V5KAYkSWxyjMj44AZAlzAQl3rUb5Gm1Iq4HiltqoUDx1SlIRIxoQZDZD"
  case class TestAPIParams(name: String, phone: Int)

  implicit val system = ActorSystem("actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val routes: Route =
    get {
      pathSingleSlash {
        complete("Hello")
      } ~
      path("webhook") {
        parameters("hub.challenge") { (`hub.challenge`) =>
          complete(`hub.challenge`)
        }
      }
    } ~
    post {
      pathSingleSlash {
        complete("Hello Post")
      } ~
      path("webhook") {
        entity(as[String]) { json =>
          val messageType: Unit = handler.getMessageType(json) match {
            case "feed" => handler getPostInfo(json)
            case "messenger" => handler getMessageInfo(json)
            case _ => println("Webhook nao tratavel atualmente")
          }
          complete(s"{Seu objeto: \n ${json}}")
        }
      } ~
      path("adapter") {
        entity(as[JsValue]) { json =>
          println(json)
          complete(s"{Seu objeto: \n ${json.asJsObject}}")
        }
      }
    }

  akka.http.scaladsl.Http().bindAndHandle(routes, "0.0.0.0", 8080)



}
