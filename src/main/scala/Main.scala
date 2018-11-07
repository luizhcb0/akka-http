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

  def getPostInfo(json: String): Unit = {

    // Get User PSID
    val js = parse(json)
    val userId = (js \\ "value" \ "from" \ "id").values
    println(userId)
    enrichUser(userId.toString)
  }

  def getMessageInfo(json: String): Unit = {
    val p = Promise[String]()
    val f = p.future

    // Get MID Value
    val js = parse(json)
    val midVal = (js \\ "message" \ "mid").values
    val mid = "m_" + midVal
    println(mid)


    // Build URI
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
        val from = (js \\ "from" \ "id").values
        val to = ((js \\ "to" \ "data") (0) \ "id").values
        var str = "{"
        str += "\"from\":\"" + from + "\","
        str += "\"to\":\"" + to + "\","
        str += "\"message\":\"" + (js \\ "message").values + "\","
        str += "\"mid\":\"%s\"".format(mid)
        str += "}"
        println(str)
        if (from != appPSID) {
          enrichUser(from.toString)
        }
        else {
          enrichUser(to.toString)
        }
      }
    }
  }

  def enrichUser(userId: String): Unit = {
    var uri: String = s"https://graph.facebook.com/${userId}?fields=${userFields}&access_token=${pageAccessToken}"
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

  def getMessageType(json: String): String = {
    var messageType: String = ""

    val js = parse(json)
    println(json)

    val message = (js \\ "message" \ "mid").values
    val feed = (js \\ "changes" \ "field").values

    if (message != None) {
      messageType = "messenger"
    }
    else if (feed != None) {
      messageType = "feed"
    }
    else {
      messageType = "unknown"
    }
    println(messageType)
    return messageType
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
      }
    } ~
    post {
      pathSingleSlash {
        complete("Hello Post")
      } ~
      path("webhook") {
        entity(as[String]) { json =>
          val messageType: Unit = getMessageType(json) match {
            case "feed" => getPostInfo(json)
            case "messenger" => getMessageInfo(json)
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
