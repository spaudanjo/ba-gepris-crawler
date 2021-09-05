package gepriscrawler.stage0.resourceidstocrawl

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.{FlowShape, Graph}
import akka.util.ByteString
import org.jsoup.Jsoup

import scala.concurrent.ExecutionContext

object NumberOfResourcesGraph {

  def graph(resourceType: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[FlowShape[Cookie, Int], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val resourceNameForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
    val initialUrl = s"https://gepris.dfg.de/gepris/OCTOPUS?beginOfFunding=&bewilligungsStatus=&context=$resourceNameForUrlQuery&continentId=%23&countryKey=%23%23%23&einrichtungsart=-1&fachlicheZuordnung=%23&findButton=historyCall&gefoerdertIn=&index=0&language=en&null=All+Locations+%2F+Regions&nurProjekteMitAB=false&oldContinentId=%23&oldCountryId=%23%23%23&oldSubContinentId=%23%23&oldpeo=%23&peo=%23&subContinentId=%23%23&task=doSearchExtended&teilprojekte=true&zk_transferprojekt=false"

    val cookies = b.add(Flow[Cookie])
    val outbound = b.add(Flow[Int])

    val numberOfResources: CombinerBase[Int] = cookies.mapAsync(1) { cookie =>
      Http().singleRequest(
        HttpRequest(uri = initialUrl, headers = List(cookie))
      )
    }
      .flatMapConcat { resp: HttpResponse =>
        resp.status.intValue() match {
          case 200 =>
            resp.entity.dataBytes.fold[ByteString](ByteString(""))(_ ++ _)
              .map(fullEntityByteString => Jsoup.parse(fullEntityByteString.utf8String))
              .map(_.select("#listennavi > span:matches(Results 1 to)"))
              .map(_.text())
              .map { text =>
                val numberOfResourcesRegex = ".*out of ([\\d\\,]*) on.*".r
                val numberOfResources = text match {
                  case numberOfResourcesRegex(matchedNumberOfResources) => matchedNumberOfResources
                  case _ => ""
                }
                // TODO: For multi language support - be aware that the decimal separator will be different for German (',' instead of '.')
                numberOfResources.replace(",", "").toInt
              }
          case _ =>
            println(s"Got NON 200 status code for request for NUMBER OF RESOURCES for resource type '{$resourceType}'; resp status: ${resp.status}; resp headers: ${resp.headers}")
            throw new Exception("Got non-200 HTTP response from Gepris server")
        }
      }

    numberOfResources ~> outbound.in
    FlowShape(cookies.in, outbound.out)
  }
}
