package gepriscrawler.stage2.CrawlAndExtractInstitutions

import java.io.File
import java.nio.file.{Path, Paths}

import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink, Source}
import gepriscrawler._
import gepriscrawler.helpers.CrawlerHelpers
import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler.stage2.CrawlAndExtractInstitutions.InstitutionExtractor.InstitutionExtractorGraph

import scala.concurrent.ExecutionContext

object ExtractInstitutionsGraph {

  def graph(exportRootPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage2", "institution")
    (new File(exportPath.toString)).mkdirs()

    val importPath = Paths.get(exportRootPath.toString, "stage1", "institution")

    val extractedInstitutionDataCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "extracted_Institution_data", Institution.csvHeader)

    val institutionIds: Set[String] = CrawlerHelpers.getResourceIdsAlreadyCrawledFromCsv("institution", exportRootPath)

    val crawledInstitutionData = Source(institutionIds).map { institutionId =>
      val filePath = s"$importPath/html/$institutionId.html"
      CrawledResourceData("institution", institutionId, "en", scala.io.Source.fromFile(filePath).mkString)
    }

    val extractedInstitutionData = b.add(InstitutionExtractorGraph.graph)
    val extractedInstitutionDataBC = b.add(Broadcast[Institution](2))


    ////////////////////////// DEFINITION OF THE WORKFLOW / GRAPH

    crawledInstitutionData ~> extractedInstitutionData ~> extractedInstitutionDataBC

    extractedInstitutionDataBC.out(0).map(_.toCsvRow) ~> extractedInstitutionDataCsvWriterSink

    SourceShape(extractedInstitutionDataBC.out(1))
  }

}
