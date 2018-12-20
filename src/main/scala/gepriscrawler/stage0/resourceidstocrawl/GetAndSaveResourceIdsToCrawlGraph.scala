package gepriscrawler.stage0.resourceidstocrawl

import java.io.File
import java.nio.file.{Files, Path, Paths}

import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler.helpers.{CookieFlowGraph, CrawlerHelpers}
import akka.NotUsed
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.scaladsl.{Balance, Broadcast, GraphDSL, Sink, Source, Zip}
import akka.stream.{FlowShape, Graph, SourceShape}
import com.github.tototoshi.csv.CSVReader

import scala.concurrent.ExecutionContext

object GetAndSaveResourceIdsToCrawlGraph {

  def graph(exportRootPath: Path, resourceName: String, resourceLinkCssSelector: String = "div.results > h2 > a")(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[SourceShape[(String, String)], NotUsed] = GraphDSL.create() { implicit b =>
    import akka.stream.scaladsl.GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage0", resourceName)
    (new File(exportPath.toString)).mkdirs()

    def allResourceIdsAlreadyFetched(): Boolean = {
      //TODO: consider to write more idiomatic Scala here instead of nested IFs and explicit returns
      val numberOfResourcesToCrawlTxtPath = s"$exportPath/number_of_${resourceName}s_to_crawl.txt"
      val resourceIdsAndNamesCsvPath = s"$exportPath/${resourceName}_ids_and_names.csv"
      if(Files.exists(Paths.get(numberOfResourcesToCrawlTxtPath)) && Files.exists(Paths.get(resourceIdsAndNamesCsvPath))) {
        val numberOfResourcesToCrawlFromTxtValue = scala.io.Source.fromFile(numberOfResourcesToCrawlTxtPath).getLines.toList.head.toInt
        val numberOfResourceIdsInCsvFile = CSVReader.open(new File(resourceIdsAndNamesCsvPath)).all().length - 1 // -1 for the first row, which is the header
        return numberOfResourceIdsInCsvFile == numberOfResourcesToCrawlFromTxtValue
      }
      return false
    }


    if(allResourceIdsAlreadyFetched()) {
      b.add(Source.empty)
      //TODO: for testing  b.add(Source(List(("153458519", "153458519"))))
    }

    else {
      val resourceIdsAndNameCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.CsvFileWriterSinkCreator.create(exportPath, s"${resourceName}_ids_and_names", Seq(s"${resourceName}_id", "name"), append = false)

      val cookiePinger: SourceShape[Boolean] = b.add(Source.repeat(false))
      val cookieFlow: FlowShape[Boolean, Cookie] = b.add(CookieFlowGraph.graph)

      val numberOfResources = b.add(NumberOfResourcesGraph.graph(resourceName))
      //TODO: for testing   val numberOfResources = b.add(Source.single(200))
      val numberOfResourcesBC = b.add(Broadcast[Int](2))
      val paginatedResourceCatalogUrls = b.add(PaginatedResourceCatalogUrlsGraph.graph(resourceName))
      val resourceCatalogPagesToResourceIdsToCrawl: FlowShape[(Cookie, String), (String, String)] = b.add(ResourceCatalogPagesToResourceIdsToCrawlGraph.graph(resourceName, resourceLinkCssSelector))
      val zipCookiesWithPaginatedResourceCatalogUrls = b.add(Zip[Cookie, String])
      val cookieBalancer = b.add(Balance[Cookie](2))
      val resourceIdsAndNamesBC = b.add(Broadcast[(String, String)](2))

      val numberOfResourceIdsToStatusFileSink = CrawlerHelpers.createTextFileWriterSink(s"$exportPath/number_of_${resourceName}s_to_crawl.txt")
      cookiePinger ~> cookieFlow ~> cookieBalancer
      cookieBalancer.out(0).take(1) ~> numberOfResources.in

      numberOfResources.out ~> numberOfResourcesBC
      //TODO: for testing      .map(_ => 100)

      numberOfResourcesBC.out(0) ~> paginatedResourceCatalogUrls.in
      numberOfResourcesBC.out(1).map(_.toString) ~> numberOfResourceIdsToStatusFileSink
      paginatedResourceCatalogUrls.out ~> zipCookiesWithPaginatedResourceCatalogUrls.in1
      cookieBalancer.out(1) ~> zipCookiesWithPaginatedResourceCatalogUrls.in0

      zipCookiesWithPaginatedResourceCatalogUrls.out ~> resourceCatalogPagesToResourceIdsToCrawl.in

      resourceCatalogPagesToResourceIdsToCrawl ~> resourceIdsAndNamesBC
      resourceIdsAndNamesBC.out(0).map(x => Seq(x._1, x._2)) ~> resourceIdsAndNameCsvWriterSink

      //TODO: for testing    FlowShape(cookieFlow.in, resourceCatalogPagesToResourceIdsToCrawl.take(1).map(_ => ("173975782", "FOO TITLE")).outlet)
      SourceShape(resourceIdsAndNamesBC.out(1))
    }
  }
}
