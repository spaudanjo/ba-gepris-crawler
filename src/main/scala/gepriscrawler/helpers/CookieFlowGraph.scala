package gepriscrawler.helpers

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.scaladsl.Flow

import scala.concurrent.ExecutionContext

object CookieFlowGraph {

  // This graph stage creates  is based on the staefulMapConcat operator of akka-streams.
  // It initially creates a cookie by calling the Gepris website once.
  // The HTTP response should have a HTTP header with the name "Set-Cookie", with the new cookie as its value.
  // This cookie is stored then in the (stateful/mutable) variable 'latestCookie'.
  // Whenever this stage receives a 'false', it sends out the cooke value stored in 'latestCookie'.

  // Even though not used at the moment, the operator also allows to fetch a new cookie.
  // It does that when it receives a 'true' instead of a 'false'.
  // We could use this for example for a feature that would recognise an invalid cookie response from the server
  // and retries the latest HTTP call to the Gepris website, but with sending once a 'true' to this stage,
  // which would then result in an extra request to the Gepris website to fetch the new cookie which would then override
  // the existing cookie value in 'latestCookie'.
  // At the moment, when we end up with a invalid cookie response, the crawler just stops working correctly
  // and it needs to be restarted (even though it can resume by restarting only the last stage where it was in).

  // Ideally, this file should be the only one in this code base which is using a 'var' statement (instead of immutable 'val's),
  // which means it is stateful.
  def graph(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Flow[Boolean, Cookie, NotUsed] =
    Flow[Boolean].statefulMapConcat({ () =>

    val requestNewCookie = () =>
      Http().singleRequest(
        HttpRequest(
          uri = s"https://gepris.dfg.de/gepris/projekt/123456",
          headers = List(Cookie("JSESSIONID", "INITIAL-DUMMY-SESSION"))
        )
      ).map(resp => resp.headers.find(_.name() == "Set-Cookie").map { cookie =>
        val valueAfterEqual = cookie.value.split("=")(1)
        valueAfterEqual.split(";")(0)
      })
        .map(cookieOption => cookieOption.getOrElse(throw new Exception("No cookie could be extracted")))
        .map(Cookie("JSESSIONID", _))

    var latestCookie = requestNewCookie()

    newCookieFlag => {
      if(newCookieFlag) {
        latestCookie = requestNewCookie()
      }
      List(
        latestCookie
      )
    }
  })
    .mapAsync(1)(x => x)

}
