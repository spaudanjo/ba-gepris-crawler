package gepriscrawler.stage2

import java.io.File
import java.nio.file.{Path, Paths}

import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler._
import gepriscrawler.helpers.CrawlerHelpers
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import akka.stream.{FlowShape, SourceShape}
import com.github.tototoshi.csv.CSVReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext


object GenericFieldExtractorGraph {

  def getIdsForResourceTypeFromCsv(exportRootPath: Path, resourceType: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = {
    val csvPathResourceIds: Path = Paths.get(exportRootPath.toString, "stage1", resourceType, s"crawled_${resourceType}_ids.csv")
    val readerResourceIds = CSVReader.open(new File(csvPathResourceIds.toString))

    val resourceIds = readerResourceIds.all()
      // ignore the first header line
      .drop(1)
      // get the first and only column, which is the resource-id
      .map(_ (0))
      .toSet

    readerResourceIds.close()

    resourceIds
  }

  // this graphs extracts for all ressources (so atm these are projects, institutions and persons)
  // all field names and the respective values by a list of generic css classes/ids
  // it will generate a CSV that has the following columns:
  // resource_type (e.g. 'project')
  // resource_id (e.g. '1332044')
  // field_name (e.g. 'Person')
  // field_value (e.g. 'Prof. Max Mustermann')
  def graph(exportRootPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage2")
    (new File(exportPath.toString)).mkdirs()

    val resourceTypes = gepriscrawler.GeprisResources.resourceList.map(_._1).toList

    val genericFieldExtractionsCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(
      exportPath,
     "generic_field_extractions",
      List("resource_type", "resource_id", "field_name", "field_value")
    )

    case class ResourceIdentifier(resourceType: String, resourceId: String)

    val resourceTypesAndIdsAndHtml: List[ResourceIdentifier] = resourceTypes.flatMap { rt =>
      getIdsForResourceTypeFromCsv(exportRootPath, rt).map { id =>
        ResourceIdentifier(rt, id)
      }
    }

    val extractFieldsGraph = Flow[ResourceIdentifier]
      .mapConcat { resourceTriple: ResourceIdentifier =>

        val filePath = s"$exportRootPath/stage1/${resourceTriple.resourceType}/html/${resourceTriple.resourceId}.html"
        val html = scala.io.Source.fromFile(filePath).mkString

        val body: Document = Jsoup.parse(html)
        val detailSection = body.select("#detailseite > div > div > div.content_frame > div.detailed")

        val namesAndValues = (detailSection
          .select(".name").eachText().asScala.map { name =>
          val value = detailSection.select(s".name:matches($name) + span").html()
          (name, value)
        } :+
          (
            detailSection.select("#tabbutton1 > a > span").text(),
            detailSection.select(".tab1 + div.content_frame > div").text()
          )).toVector

        print(s"\rExtracted all fields by generic selector approach for resource: '${resourceTriple.resourceType}' - '${resourceTriple.resourceId}'                                                   ")
        namesAndValues.map(nameAndValue => Vector(resourceTriple.resourceType, resourceTriple.resourceId, nameAndValue._1, nameAndValue._2))
      }

    val resourceTypesAndIdsAndHtmlGraph = Source(resourceTypesAndIdsAndHtml)
    val extractedFields: FlowShape[ResourceIdentifier, Vector[String]] = b.add(extractFieldsGraph)
    val extractedFieldsBC = b.add(Broadcast[Vector[String]](2))

    ////////////////////////// DEFINITION OF THE WORKFLOW / GRAPH
    resourceTypesAndIdsAndHtmlGraph ~> extractedFields ~> extractedFieldsBC
    extractedFieldsBC.out(0) ~> genericFieldExtractionsCsvWriterSink

    SourceShape(extractedFieldsBC.out(1))
  }

}
