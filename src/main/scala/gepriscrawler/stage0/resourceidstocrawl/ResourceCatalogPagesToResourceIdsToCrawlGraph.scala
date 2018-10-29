package gepriscrawler.stage0.resourceidstocrawl

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.{FlowShape, Graph}
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.util.ByteString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object ResourceCatalogPagesToResourceIdsToCrawlGraph {
  def graph(resourceType: String, resourceLinkCssSelector: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    val resourceIdToName = b.add(Flow[(Cookie, String)]
      .mapAsync(20) { case (cookie, url) =>
        val indexRegex = ".*index=(\\d*)&.*".r
        val indexRegex(index) = url
        Http().singleRequest(
          request = HttpRequest(
            uri = url,
            headers = List(cookie) //,

          )).zip(Future.successful(url)) //, settings = settings)
      }.flatMapConcat { case (resp: HttpResponse, url: String) =>
      resp.status.intValue() match {
        case 200 => {
          val indexRegex = ".*index=(\\d*)&.*".r
          val indexRegex(index) = url
          resp.entity.dataBytes.fold[ByteString](ByteString(""))(_ ++ _)
            .map(fullEntityByteString => Jsoup.parse(fullEntityByteString.utf8String).body())
        }
        case _ => {
          println(s"Got non 200 status code for ${resourceType} for resp")
          throw new Exception("Got non-200 HTTP response from Gepris server")
        }
      }
    }
      .mapConcat { pageContent =>
        val resourceLinks: scala.collection.immutable.Seq[Element] = pageContent.select(resourceLinkCssSelector).asScala.to[collection.immutable.Seq]

        val resourceIdsToNames: scala.collection.immutable.Seq[(String, String)] = resourceLinks.map { resourceLink =>

          val resourceName = resourceLink.text()
          val resourceTypeForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
          val resourceHref = resourceLink.attr("href")
          val resourceRegex = raw"""\/gepris\/${resourceTypeForUrlQuery}/(\d*)""".r
          val resourceId = resourceHref match {
            case resourceRegex(id) => id
            case _ => ""
          }

          print(s"\rReceived from catalog page the following resource for resource type '${resourceType}': id: $resourceId, name: $resourceName                         ")
          (resourceId -> resourceName)
        }

        resourceIdsToNames
      }
    )

    FlowShape(resourceIdToName.in, resourceIdToName.out)
  }

}
