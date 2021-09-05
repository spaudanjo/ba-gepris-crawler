package gepriscrawler.stage1.crawlresourcedetails

import gepriscrawler.CrawledResourceData
import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.{FlowShape, Graph}
import akka.util.ByteString
import org.jsoup.Jsoup

import scala.concurrent.ExecutionContext

object CrawledResourceDetailDataGraph
{

  def graph(resourceType: String, crawledLanguage: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[FlowShape[(Cookie, String), CrawledResourceData], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val resourceNameForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
    val cookieAndUrlToCrawl = b.add(Flow[(Cookie, String)])
    val resourceDetailPageHttpRequest = b.add(Flow[(Cookie, String)]
      .mapAsync(10) { case (cookie, resourceId) =>
        Http().singleRequest(
          HttpRequest(
            uri = s"https://gepris.dfg.de/gepris/${resourceNameForUrlQuery}/$resourceId?language=$crawledLanguage",
            headers = List(cookie)
          )
        )
          .map((resourceId, _))
      }
    )

    val crawledResourceDetailData: FlowShape[(String, HttpResponse), CrawledResourceData] = b.add(Flow[(String, HttpResponse)]
      .flatMapConcat { case (resourceId: String, resp: HttpResponse) =>
        resp.status.intValue() match {
          case 200 =>
            print(s"\rReceived the detail page for the following resource for resource type '${resourceType}': id: $resourceId                                                   ")
            resp.entity.dataBytes.fold[ByteString](ByteString(""))(_ ++ _)
              .map(fullEntityByteString => (resourceId -> Jsoup.parse(fullEntityByteString.utf8String).body()))
              .map(s => CrawledResourceData(resourceType, resourceId, crawledLanguage, s._2.toString))
          case _ =>
            println(s"Got non 200 status code for resource with id $resourceId; resp status: ${resp.status}; resp headers: ${resp.headers}")
            throw new Exception("Got non-200 HTTP response from Gepris server")
//            Source.empty
        }
      }
    )

    cookieAndUrlToCrawl ~> resourceDetailPageHttpRequest ~> crawledResourceDetailData
    FlowShape(cookieAndUrlToCrawl.in, crawledResourceDetailData.out)
  }
}
