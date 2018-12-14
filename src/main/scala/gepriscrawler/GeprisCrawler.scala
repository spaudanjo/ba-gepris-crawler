package gepriscrawler

import java.io.File
import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import gepriscrawler.helpers.CrawlerHelpers
import gepriscrawler.stage0.CrawlAndExtractSubjectAreasGraph
import gepriscrawler.stage0.resourceidstocrawl.GetAndSaveResourceIdsToCrawlGraph
import gepriscrawler.stage1.crawlresourcedetails.CrawlResourceDetailsGraph
import gepriscrawler.stage2.GenericFieldExtractorGraph

package object GeprisCrawler {

  private def getListOfSubDirectories(directoryName: String): Seq[String] = {
    (new File(directoryName))
      .listFiles
      .filter(_.isDirectory)
      .map(_.getName)
  }

  private def determineStageLevelToStartFrom(exportPath: Path) = {
    val listOfStageDirectories = getListOfSubDirectories(exportPath.toString)
    if (listOfStageDirectories.contains("stage2")) 2
    else if (listOfStageDirectories.contains("stage1")) 1
    else 0
  }

  private def runCrawler(exportPath: Path, crawlName: String, stageLevelToStartFromByUser: Option[Int]) = {

    // The crawler uses akka-streams a lot, which relies on akka.
    // For that, we need to initialize an Akka actor system and an materializer.
    // We mark them as implicit values, so that they are 'magically' passed in as the last parameter
    // to all functions that rely on them.
    // Also, we import the default dispatcher/ExecutionContext (for managing multi threaded exection aspects) of the actor system.
    implicit val actorSystem = ActorSystem("akka-system")
    implicit val flowMaterializer = ActorMaterializer()
    implicit val executionContext = actorSystem.dispatcher

    // STAGE 0 (CRAWL AND EXTRACT Subject Areas and List and Names of Detail ressources to crawl)
    def stage0(s: Any) = {
      println("Go on with stage0")
      CrawlerHelpers.deleteAndCreateStageFolderByStageNumber(exportPath, 0)

      Source.fromGraph(CrawlAndExtractSubjectAreasGraph.graph(exportPath)).runWith(Sink.ignore)
        .andThen {
          case(_) => println("Done with crawling and extracting the subject_areas (from the official DFG Fachsystematik)")
        }

        .flatMap(_ => Source.fromGraph(GetAndSaveResourceIdsToCrawlGraph.graph(exportPath, "project")).runWith(Sink.ignore))
        .andThen {
          case(_) => println("Done with crawling all catalog pages and extracting all resource ids to consider for crawling for: projects")
        }

        .flatMap(_ => Source.fromGraph(GetAndSaveResourceIdsToCrawlGraph.graph(exportPath, "person")).runWith(Sink.ignore))
        .andThen {
          case(_) => println("Done with crawling all catalog pages and extracting all resource ids to consider for crawling for: persons")
        }

        .flatMap(_ => Source.fromGraph(GetAndSaveResourceIdsToCrawlGraph.graph(exportPath, "institution", "div.subInstitution > a")).runWith(Sink.ignore))
        .andThen {
          case(_) => println("Done with crawling all catalog pages and extracting all resource ids to consider for crawling for: institutions")
        }

        .andThen {
          case(_) => println("Done with STAGE 0")
        }
    }

    // STAGE 1 (CRAWL DETAIL PAGES)
    def stage1(s: Any) = {
      println("Go on with stage1")
      // For stage 1, where the main portion of the crawling is happening, we don't want to delete the folder with already crawled pages.
      CrawlerHelpers.deleteAndCreateStageFolderByStageNumber(exportPath, 1, deleteExistingFolderBefore = false)

      Source.fromGraph(CrawlResourceDetailsGraph.graph("person", exportPath)).runWith(Sink.ignore)
        .andThen {
          case(_) => println("Done with crawling the detail pages for: persons")
        }

        .flatMap(_ => Source.fromGraph(CrawlResourceDetailsGraph.graph("project", exportPath)).runWith(Sink.ignore))
        .andThen {
          case(_) => println("Done with crawling the detail pages for: projects")
        }

        .flatMap(_ => Source.fromGraph(CrawlResourceDetailsGraph.graph("institution", exportPath)).runWith(Sink.ignore))
        .andThen {
          case(_) => println("Done with crawling the detail pages for: institutions")
        }

        .andThen {
          case(_) => println("Done with STAGE 1")
        }
    }

    // STAGE 2 (EXTRACTION)
    def stage2(s: Any) = {
      println("Go on with stage2")
      CrawlerHelpers.deleteAndCreateStageFolderByStageNumber(exportPath, 2)
      Source.fromGraph(GenericFieldExtractorGraph.graph(exportPath)).runWith(Sink.ignore)
        .andThen {
          case(_) => println("Done with the generic field extraction of all resources (projects, people, institutions).")
        }

        .andThen{
          case(_) => println("Done with STAGE 2.")
        }
    }



    val stageLevelToStartFrom = stageLevelToStartFromByUser match {
      case Some(level) => level
      case None => determineStageLevelToStartFrom(exportPath)
    }

    println(s"Folder name of the crawl: $crawlName")

    (stageLevelToStartFrom match {
      case (0) => stage0().flatMap(stage1).flatMap(stage2)
      case (1) => stage1().flatMap(stage2)
      case (2) => stage2()
    })
      .onComplete { status =>
        val successOrFailure = if(status.isSuccess) "successfully" else "by failure"
        println(s"Crawler finished $successOrFailure")

        println(s"folder name of the crawl: $crawlName")

        println(s"Terminating the akka actor-system, status: $status")
        actorSystem.terminate()
      }
  }


  def resumeExistingCrawl(exportRootPath: String, stageToStartFrom: Option[Int]) {
    val exportPath = Paths.get(exportRootPath)
    val exportFolderNameWithDate = exportPath.getFileName().toString

    runCrawler(exportPath, exportFolderNameWithDate, stageToStartFrom)
  }

  def startNewCrawl(exportRootPath: String) = {
    val exportFolderNameWithDate = s"${CrawlerHelpers.currentDateTimeStr}-EN"

    // This is the top-level folder name. The folder will be created if it doens't exist yet
    // and all interim and output artefacts (like the CSV exports) will be saved here.
    val exportPath: Path = Paths.get(exportRootPath, exportFolderNameWithDate)
    (new File(exportPath.toString)).mkdirs()

    runCrawler(exportPath, exportFolderNameWithDate, None)
  }

}