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

  val userFields = "name,email,hometown,id,gender,birthday,picture"
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

  def getMessageInfo(mid: String): Unit = {
    val p = Promise[String]()
    val f = p.future
    val uri = s"https://graph.facebook.com/${mid}?fields=${messageFields}&access_token=${pageAccessToken}"
    println(uri)
    val producer = Future {
      val response: scalaj.http.HttpResponse[String] = scalaj.http.Http(uri).asString
      p success response.body
    }
    val consumer = Future {
      f foreach { response =>
        val js = parse(response)
        println(response)
        var str = "{"
        str += "\"from\":\"" + (js \\ "from" \ "id").values + "\","
        str += "\"to\":\"" + ((js \\ "to" \ "data") (0) \ "id").values + "\","
        str += "\"message\":\"" + (js \\ "message").values + "\","
        str += "\"mid\":\"%s\"".format(mid)
        str += "}"
        println(str)
        enrichUser(str)
      }
    }
  }

  def enrichUser(str: String): Unit = {
    var uri: String = ""
    val js = parse(str)
    val from = (js \\ "from").values.toString
    val to = (js \\ "to").values.toString
    if (from != appPSID) {
      uri = s"https://graph.facebook.com/${from}?fields=${userFields}&access_token=${pageAccessToken}"
    }
    else {
      uri = s"https://graph.facebook.com/${to}?fields=${userFields}&access_token=${pageAccessToken}"
    }
    println(uri)
    val futureGet = Future {
      val response: scalaj.http.HttpResponse[String] = scalaj.http.Http(uri).asString
      response.body
    }
    futureGet onComplete {
      case Success(value) => {
        println(value)
      }
      case Failure(exception) => println(exception)
    }
  }


  val routes: Route =
    get {
      pathSingleSlash {
        complete("Hello")
      } ~
      path("webhook") {
        parameters("hub.challenge") { (`hub.challenge`) =>
          complete(`hub.challenge`)
        }
      } ~
      path("crash") {
        complete("BOOM!")
      }
    } ~
    post {
      pathSingleSlash {
        complete("Hello Post")
      } ~
      path("webhook") {
        entity(as[String]) { json =>
          val js = parse(json)
          println(json)
          val midVal = (js \\ "message" \ "mid").values
          if (midVal != None) {
            val mid = "m_" + midVal
            println(mid)
            val str = getMessageInfo(mid)
//            enrichUser(str)
          }else {
            println("Webhook nao proveniente do messenger")
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
