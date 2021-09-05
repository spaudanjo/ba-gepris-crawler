package gepriscrawler.stage0

import java.nio.file.{Path, Paths}

import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler._
import gepriscrawler.helpers.{CookieFlowGraph, CrawlerHelpers}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}
import akka.util.ByteString
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object CrawlAndExtractSubjectAreasGraph {

  def graph(exportRootPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage0")
    val subjectAreasCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(
      exportPath, "subject_areas",
      Seq("subject_area", "review_board", "research_area")
    )

    val cookiePinger: SourceShape[Boolean] = b.add(Source.repeat(false))
    val cookieFlow: FlowShape[Boolean, Cookie] = b.add(CookieFlowGraph.graph)

    // TODO: multi langauge support
    val initialSubjectAreaUrl = b.add(Source.single("https://www.dfg.de/en/dfg_profile/statutory_bodies/review_boards/subject_areas/index.jsp"))
//    val initialSubjectAreaUrl = b.add(Source.single("https://www.dfg.de/dfg_profil/gremien/fachkollegien/faecher/"))

    val subjectAreaRequestAndExtractorBC = b.add(Broadcast[ResearchArea](2))

    val subjectAreaRequestAndExtractor: FlowShape[(String, Cookie), ResearchArea] = b.add(Flow[(String, Cookie)]
      .mapAsync(6) { case (subjectAreaUrl, cookie) =>
        Http().singleRequest(
          HttpRequest(
            uri = subjectAreaUrl,
            headers = List(cookie)
          )
        )
      }
      .flatMapConcat { resp: HttpResponse =>
        resp.status.intValue() match {
          case 200 =>
            resp.entity.dataBytes.fold[ByteString](ByteString(""))(_ ++ _)
              .map(fullEntityByteString => Jsoup.parse(fullEntityByteString.utf8String).body())
              .map { body =>
                val researchAreasElements = body.select("#main-content > div:nth-child(2) > div > div > div > div > div > table").asScala
                researchAreasElements.map { researchAreaElement =>
                  val researchAreaTitle = researchAreaElement.select("caption").text()
                  val reviewBoardElements = researchAreaElement.select(".fachkolleg").asScala

                  val reviewBoards = reviewBoardElements.map { reviewBoardElement =>
                    val reviewBoardTitle = reviewBoardElement.select("td > a:not(.toggle_fk)").text()
                    val subjectAreas = reviewBoardElement.select(".fachInhalt > a").eachText().asScala.map(_.trim)
                    ReviewBoard(reviewBoardTitle.trim, subjectAreas)
                  }
                  ResearchArea(researchAreaTitle.trim, reviewBoards)
                }.toList
              }
          case _ =>
            println(s"Got non 200 status code for subject area crawling; resp status: ${resp.status}; resp headers: ${resp.headers}")
            throw new Exception("Got non-200 HTTP response from Gepris server")
        }
      }
        .mapConcat(x => x)
    )

    val cookieWithsubjectAreaUrlIdZipper = b.add(Zip[String, Cookie])

    initialSubjectAreaUrl ~> cookieWithsubjectAreaUrlIdZipper.in0
    cookiePinger ~> cookieFlow ~> cookieWithsubjectAreaUrlIdZipper.in1
    cookieWithsubjectAreaUrlIdZipper.out ~> subjectAreaRequestAndExtractor

    subjectAreaRequestAndExtractor ~> subjectAreaRequestAndExtractorBC

    subjectAreaRequestAndExtractorBC.out(0)
      .mapConcat { researchArea: ResearchArea =>
        researchArea.reviewBoards.flatMap { reviewBoard: ReviewBoard =>
          reviewBoard.subjectAreas.map { subjectArea: String =>
            Seq(subjectArea, reviewBoard.name, researchArea.name)
          }
        }.toList
      } ~> subjectAreasCsvWriterSink

    SourceShape(subjectAreaRequestAndExtractorBC.out(1))
  }

}
