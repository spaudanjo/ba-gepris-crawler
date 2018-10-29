package gepriscrawler.stage2.CrawlAndExtractPersons

import java.io.File
import java.nio.file.{Path, Paths}

import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink, Source}
import gepriscrawler.helpers.CrawlerHelpers
import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler.stage2.CrawlAndExtractPersons.PersonExtractor.PersonExtractorGraph
import gepriscrawler.{CrawledResourceData, Person}

import scala.concurrent.ExecutionContext

object ExtractPersonsGraph {

  def graph(exportRootPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage2", "person")
    (new File(exportPath.toString)).mkdirs()

    val importPath = Paths.get(exportRootPath.toString, "stage1", "person")

    val extractedPersonDataCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "extracted_person_data", Person.csvHeader)

    val personIds: Set[String] = CrawlerHelpers.getResourceIdsAlreadyCrawledFromCsv("person", exportRootPath)

    val crawledPersonData = Source(personIds).map { personId =>
      val filePath = s"$importPath/html/$personId.html"
      CrawledResourceData("person", personId, "en", scala.io.Source.fromFile(filePath).mkString)
    }

    val extractedPersonData = b.add(PersonExtractorGraph.graph)
    val extractedPersonDataBC = b.add(Broadcast[Person](2))


    ////////////////////////// DEFINITION OF THE WORKFLOW / GRAPH

    crawledPersonData ~> extractedPersonData ~> extractedPersonDataBC

    extractedPersonDataBC.out(0).map(_.toCsvRow) ~> extractedPersonDataCsvWriterSink

    SourceShape(extractedPersonDataBC.out(1))
  }

}
