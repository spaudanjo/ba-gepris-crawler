package gepriscrawler.stage1.crawlresourcedetails

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.{FlowShape, Graph}
import akka.util.ByteString

import scala.concurrent.ExecutionContext

object CrawledResourceDetailDataFromJsonAPIGraph
{

  def graph(resourceType: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[FlowShape[String, (String, String)], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val resourceNameForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
    val resourceIdToCrawl = b.add(Flow[String])
    val resourceDetailJsonHttpRequest = b.add(Flow[String]
      .mapAsync(2) { resourceId =>
        Http().singleRequest(
          HttpRequest(
            uri = s"https://gepris-extern.dfg.de/$resourceNameForUrlQuery/$resourceId"
          )
        )
          .map((resourceId -> _))
      }
    )

    val crawledResourceDetailJSON: FlowShape[(String, HttpResponse), (String, String)] = b.add(Flow[(String, HttpResponse)]
      .flatMapConcat { case (resourceId: String, resp: HttpResponse) =>
        resp.status.intValue() match {
          case 200 =>
            resp.entity.dataBytes.fold[ByteString](ByteString(""))(_ ++ _)
              .map(fullEntityByteString => (resourceId -> fullEntityByteString.utf8String))
          case _ =>
            println(s"Got non 200 status code for resource with id $resourceId; resp status: ${resp.status}; resp headers: ${resp.headers}")
            throw new Exception("Got non-200 HTTP response from Gepris server")
        }
      }
    )

    resourceIdToCrawl ~> resourceDetailJsonHttpRequest ~> crawledResourceDetailJSON
    FlowShape(resourceIdToCrawl.in, crawledResourceDetailJSON.out)
  }
}
