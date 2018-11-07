import Main.{messageFields, pageAccessToken, userFields, appPSID, executionContext}
import org.json4s.native.JsonMethods.parse

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class MessageHandler {

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

}
