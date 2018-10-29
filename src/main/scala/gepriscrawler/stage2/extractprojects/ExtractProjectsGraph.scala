package gepriscrawler.stage2.extractprojects

import java.io.File
import java.nio.file.{Path, Paths}

import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import akka.stream.{FlowShape, SourceShape}
import gepriscrawler._
import gepriscrawler.helpers.CrawlerHelpers
import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler.stage2.CrawlProjects.ProjectExtractor.ProjectExtractorGraph

import scala.concurrent.ExecutionContext

object ExtractProjectsGraph {

  def graph(exportRootPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage2", "project")
    (new File(exportPath.toString)).mkdirs()

    val extractedProjectDataCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "extracted_project_data", Project.csvHeader)
    val projectIdsToSubjectAreasSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "project_ids_to_subject_areas", Seq("project_id", "subject_area"))
    val internationalConnectionCsvSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "projects_international_connections", Seq("project_id", "country"))
    val projectIdsToParticipatingSubjectAreasSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "project_ids_to_participating_subject_areas", Seq("project_id", "participating_subject_area"))
    val projectPersonRelationsSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "project_person_relations", Seq(
      "project_id",
      "person_id",
      "relation_type"
    ))
    val projectInstitutionRelationsSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(exportPath, "project_institution_relations", Seq(
      "project_id",
      "institution_id",
      "relation_type"
    ))

    val importPath = Paths.get(exportRootPath.toString, "stage1", "project")

    val projectIds: Set[String] = CrawlerHelpers.getResourceIdsAlreadyCrawledFromCsv("project", exportRootPath)

    val crawledProjectData = Source(projectIds).map { projectId =>
      val filePath = s"$importPath/html/$projectId.html"
      CrawledResourceData("project", projectId, "EN", scala.io.Source.fromFile(filePath).mkString)
    }

    val projectIdsToSubjectAreas = b.add(Flow[Project].mapConcat { x =>
      x.subjectAreas.map(sA => Seq(x.projectIdNumber, sA)).toList
    })

    val projectIdsToParticipatingSubjectAreas: FlowShape[Project, CSVRow] = b.add(Flow[Project].mapConcat { x =>
      x.participatingSubjectAreas.map(pSA => Seq(x.projectIdNumber, pSA)).toList
    })
    val projectIdsToCountriesForInternationalConnection: FlowShape[Project, CSVRow] = b.add(Flow[Project].mapConcat { x =>
      x.internationalConnections.map(country => Seq(x.projectIdNumber, country)).toList
    })

    val personRelationsToCsv: FlowShape[Project, CSVRow] = b.add(Flow[Project].mapConcat { x =>
      x.personProjectRelations.map { relation =>
        val personId = relation._1
        val relationType = relation._2
        Seq(x.projectIdNumber, personId, relationType)
      }.toList
    })

    val institutionRelationsToCsv: FlowShape[Project, CSVRow] = b.add(Flow[Project].mapConcat { x =>
      x.institutionProjectRelations.map { relation =>
        val institutionId = relation._1
        val relationType = relation._2
        Seq(x.projectIdNumber, institutionId, relationType)
      }.toList
    })


    val extractedProjectData = b.add(ProjectExtractorGraph.graph)
    val extractedProjectDataBC = b.add(Broadcast[Project](7))


    ////////////////////////// DEFINITION OF THE WORKFLOW / GRAPH

    crawledProjectData ~> extractedProjectData ~> extractedProjectDataBC

    extractedProjectDataBC.out(0).map(_.toCsvRow) ~> extractedProjectDataCsvWriterSink
    extractedProjectDataBC.out(1) ~> projectIdsToSubjectAreas ~> projectIdsToSubjectAreasSink
    extractedProjectDataBC.out(2) ~> projectIdsToParticipatingSubjectAreas ~> projectIdsToParticipatingSubjectAreasSink
    extractedProjectDataBC.out(3) ~> personRelationsToCsv ~> projectPersonRelationsSink
    extractedProjectDataBC.out(4) ~> institutionRelationsToCsv ~> projectInstitutionRelationsSink
    extractedProjectDataBC.out(5) ~> projectIdsToCountriesForInternationalConnection ~> internationalConnectionCsvSink

    SourceShape(extractedProjectDataBC.out(6))
  }

}
