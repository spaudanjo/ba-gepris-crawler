package gepriscrawler.stage2.CrawlProjects.ProjectExtractor

import gepriscrawler.helpers.ExtractorHelpers
import gepriscrawler.stage2.extractprojects.projectextractor.ProjectInstitutionRelationsExtractors
import gepriscrawler.{CrawledResourceData, Project}
import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._

object ProjectExtractorGraph {

  val graph: Flow[CrawledResourceData, Project, NotUsed] = Flow[CrawledResourceData]
    .map { crawledProjectData: CrawledResourceData =>

      val body = Jsoup.parse(crawledProjectData.html).body()
      val detailSection = body.select("#detailseite > div > div > div.content_frame > div.content_inside.detailed > div.details").first()
      val allNameFields: Seq[Element] = body.select("span.name").asScala

      val title = detailSection
        .select("h1")
        .text()

      val projectDescription = body
        .select("#projektbeschreibung > #projekttext")
        .text()

      val dfgProgramme = body
        .select("#projektbeschreibung span.name:matches(DFG Programme) + span.value")
        .text()


      val subjectAreas: Seq[String] = ExtractorHelpers.extractMultivaluesByFieldNames(detailSection, Seq("Subject Area"))
      val internationalConnection = ExtractorHelpers.extractMultivaluesByFieldNames(body, Seq("International Connection"))
      val participatingSubjectAreasSplitted = ExtractorHelpers.extractMultivaluesByFieldNames(body, Seq("Participating subject areas"))

      val (start, end) = ExtractorHelpers.extractFundingDateRange(body)

      val parentProjectId = ExtractorHelpers
        .extractResourceIdsFromLinkByResourceTypeAndRegex(allNameFields)("projekt")(Seq("Subproject of"))
        .headOption.getOrElse("")

      val personProjectRelations = ProjectPersonRelationsExtractors.extractProjectPersonRelations(allNameFields)
      val institutionProjectRelations = ProjectInstitutionRelationsExtractors.extractProjectInstitutionRelations(allNameFields)

      Project(
        crawledProjectData.resourceId,
        title = title,
        projectDescription,
        subjectAreas = subjectAreas,
        dfgVerfahren = dfgProgramme,
        fundingStartYear = start,
        fundingEndYear = end,
        parentProjectId = parentProjectId,
        personProjectRelations = personProjectRelations,
        institutionProjectRelations = institutionProjectRelations,
        participatingSubjectAreas = participatingSubjectAreasSplitted,
        internationalConnections = internationalConnection: Seq[String],
        dfgProgrammeContactName = "TODO",
        dfgProgrammeContactUrl = "TODO"
      )
    }
}
