package gepriscrawler.stage1.crawlresourcedetails

import java.io.File
import java.nio.file.{Files, Path, Paths}

import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler._
import gepriscrawler.helpers.{CookieFlowGraph, CrawlerHelpers}
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Sink, Source, Zip}
import akka.stream.{FlowShape, SourceShape}
import akka.util.ByteString
import com.github.tototoshi.csv.CSVReader

import scala.concurrent.ExecutionContext

object CrawlResourceDetailsGraph {


  def getResourceIdsAlreadyCrawledFromCsv(stage1Path: String, resourceType: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = {
    val csvPathResourceIdsToCrawl: Path = Paths.get(stage1Path,  s"crawled_${resourceType}_ids.csv")
    Files.exists(csvPathResourceIdsToCrawl) match {
      case true => {
        val readerResourceIdsToCrawl = CSVReader.open(new File(csvPathResourceIdsToCrawl.toString))

        val alreadyCrawledResourceIds: Set[String] = readerResourceIdsToCrawl.all()
          // ignore the first header line
          .drop(1)
          // get the first column, which is the resource-id
          .map(_ (0))
          .toSet

        readerResourceIdsToCrawl.close()

        alreadyCrawledResourceIds
      }
      case false => Set[String]()
    }
  }

  def getResourceIdsToCrawlFromCsv(resourceType: String, rootExportPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = {
    val csvPathResourceIdsToCrawl: Path = Paths.get(rootExportPath.toString, "stage0", resourceType, s"${resourceType}_ids_and_names.csv")
    val readerResourceIdsToCrawl = CSVReader.open(new File(csvPathResourceIdsToCrawl.toString))

    val stage1Path = s"$rootExportPath/stage1/$resourceType"

    val resourceIdsToCrawl = readerResourceIdsToCrawl.all()
      // ignore the first header line
      .drop(1)
      // get the first column, which is the resource-id
      .map(_ (0))
      .toSet
      .diff(getResourceIdsAlreadyCrawledFromCsv(stage1Path, resourceType))

    readerResourceIdsToCrawl.close()

    Source(resourceIdsToCrawl.toList)
//      Source(List(("153458519")))
  }


  def graph(resourceType: String, rootExportPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._


    val exportPath = Paths.get(rootExportPath.toString, "stage1", resourceType)

    val folderPathForCrawledResourcesHtml = Paths.get(exportPath.toString, "html")
    (new File(folderPathForCrawledResourcesHtml.toString)).mkdirs()

    val crawledResourceDataHtmlWriterSink = Flow[CrawledResourceData].map { crawledResourceData =>
      val filePath = Paths.get(folderPathForCrawledResourcesHtml.toString, s"${crawledResourceData.resourceId}.html")
      Source.single(ByteString(s"${crawledResourceData.html}"))
        .runWith(FileIO.toPath(filePath))
    }
      .to(Sink.ignore)


    val cookiePinger: SourceShape[Boolean] = b.add(Source.repeat(false))
    val cookieFlow: FlowShape[Boolean, Cookie] = b.add(CookieFlowGraph.graph)

    val resourceIdsToCrawl: SourceShape[String] = b.add(getResourceIdsToCrawlFromCsv(resourceType, rootExportPath))
    val resourceIdsToCrawlBC = b.add(Broadcast[String](1))

    val crawledResourceData: FlowShape[(Cookie, String), CrawledResourceData] = b.add(CrawledResourceDetailDataGraph.graph(resourceType, "en"))
    val crawledResourceDataBC = b.add(Broadcast[CrawledResourceData](3))

    val cookieWithResourceIdZipper = b.add(Zip[Cookie, String])

//    val crawledResourceFromJsonAPI = b.add(CrawledResourceDetailDataFromJsonAPIGraph.graph(resourceType))

//    val crawledResourceDataJsonWriterSink = Flow[(String, String)].map { case (resourceId: String, jsonStr: String) =>
//      val filePath = Paths.get(folderPathForCrawledResourcesHtml.toString, s"${resourceId}.json")
//      Source.single(ByteString(s"${jsonStr}"))
//        .runWith(FileIO.toPath(filePath))
//    }
//      .to(Sink.ignore)

    val csvHeader = gepriscrawler.GeprisResources.resourceList(resourceType).csvHeader

    val crawledResourceIdsCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, s"crawled_${resourceType}_ids", Seq(s"${resourceType}_id"))



    ////////////////////////// DEFINITION OF THE WORKFLOW / GRAPH
    cookiePinger ~> cookieFlow ~> cookieWithResourceIdZipper.in0

//    b.add(Source.single(("397633889", "testResourceWithInternationalConnection"))) ~> resourceIdsBC

    resourceIdsToCrawl.out ~> resourceIdsToCrawlBC
    resourceIdsToCrawlBC.out(0) ~> cookieWithResourceIdZipper.in1

    //TODO: IMPORTANT: TAKE WIEDER RAUSNEHMEN
//    resourceIdsToCrawlBC.out(1).take(0) ~> crawledResourceFromJsonAPI ~> crawledResourceDataJsonWriterSink
//    resourceIdsToCrawlBC.out(1) ~> crawledResourceFromJsonAPI ~> crawledResourceDataJsonWriterSink
    cookieWithResourceIdZipper.out ~> crawledResourceData.in
    crawledResourceData ~> crawledResourceDataBC.in

    crawledResourceDataBC.out(0) ~> crawledResourceDataHtmlWriterSink
    crawledResourceDataBC.out(1).map(crawledResourceData => Seq(crawledResourceData.resourceId)) ~> crawledResourceIdsCsvWriterSink

    SourceShape(crawledResourceDataBC.out(2))
  }

}
