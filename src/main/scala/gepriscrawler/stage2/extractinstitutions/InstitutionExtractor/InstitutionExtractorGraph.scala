package gepriscrawler.stage2.CrawlAndExtractInstitutions.InstitutionExtractor

import scala.collection.JavaConverters._
import gepriscrawler.{CrawledResourceData, Institution}
import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

object InstitutionExtractorGraph {

  val graph: Flow[CrawledResourceData, Institution, NotUsed] = Flow[CrawledResourceData]
    .map { crawledResourceData: CrawledResourceData =>

      val body: Document = Jsoup.parse(crawledResourceData.html)
      val detailSection = body.select("#detailseite > div > div > div.content_frame > div.detailed")

      val name = detailSection
        .select("h3")
        .text()

      Institution(
        crawledResourceData.resourceId,
        name = detailSection
          .select("h3")
          .text(),
        address = detailSection
          .select("span.name:matches(Address) + span")
          .html()
          .split("<br>")
          .map(_.trim)
          .mkString("\n"),
        phone = detailSection
          .select("span.name:matches(Telephone) + span")
          .text(),
        fax = detailSection
          .select("span.name:matches(Fax) + span")
          .text(),
        email = detailSection
          .select("span.name:matches(E-Mail) + span")
          .html()
          .split("<img[^<]*>")
          .mkString("@"),
        internet = detailSection
          .select("span.name:matches(Website) + span")
          .text(),
        projectIdsOnInstitutionDetailPage = detailSection
          .select("#projekteNachProgrammen a[href^=/gepris/projekt]").asScala.to[collection.immutable.Seq]
          .map { projectLink =>
            val projectHref = projectLink.attr("href")
            val projectRegex = """\/gepris\/projekt/(\d*)""".r
            val projectId = projectHref match {
              case projectRegex(id) => id
              case _ => ""
            }
            projectId
          }
      )
    }

}
