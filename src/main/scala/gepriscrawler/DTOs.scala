package gepriscrawler


trait ExtractedResourceData {
  val csvHeader: Seq[String]
  val resourceTyppeForUrlQuery: String
}

package object GeprisResources {
  val resourceList = Map(
    "project" -> Project,
    "institution" -> Institution,
    "person" -> Person
  )
}

case class Project(
                                 projectIdNumber: String,
                                 title: String,
                                 projectDescription: String,
                                 subjectAreas: Seq[String],
                                 dfgVerfahren: String,
                                 fundingStartYear: String,
                                 fundingEndYear: String,
                                 parentProjectId: String,
                                 personProjectRelations: Seq[(String, String)],
                                 institutionProjectRelations: Seq[(String, String)],
                                 participatingSubjectAreas: Seq[String],
                                 internationalConnections: Seq[String],
                                 dfgProgrammeContactName: String,
                                 dfgProgrammeContactUrl: String,

                               )
{
  def toCsvRow = Seq(projectIdNumber, title, projectDescription, dfgVerfahren, fundingStartYear, fundingEndYear, parentProjectId)
}

object Project extends ExtractedResourceData {
  val csvHeader: Seq[String] = Seq(
    "project_id",
    "title",
    "project_description",
    "dfg_programme",
    "funding_start_year",
    "funding_end_year",
    "parent_project_id"
  )
  override val resourceTyppeForUrlQuery: String = "projekt"
}

case class Person(
                                personId: String,
                                name: String,
                                institutionName: String,
                                address: String,
                                phone: String,
                                fax: String,
                                email: String,
                                internet: String
                              )
{
  def toCsvRow = Seq(personId, name, institutionName, address, phone, fax, email, internet)
}

object Person extends ExtractedResourceData {
  val csvHeader: Seq[String] = Seq(
    "person_id",
    "name",
    "institution_name",
    "address",
    "phone",
    "fax",
    "email",
    "internet"
  )
  override val resourceTyppeForUrlQuery: String = "person"
}

case class Institution(
                                institutionId: String,
                                name: String,
                                address: String,
                                phone: String,
                                fax: String,
                                email: String,
                                internet: String,
                                projectIdsOnInstitutionDetailPage: scala.collection.immutable.Seq[String]
                              )
{
  def toCsvRow: Seq[String] = Seq(institutionId, name, address, phone, fax, email, internet)
}

object Institution extends ExtractedResourceData {
  val csvHeader: Seq[String] = Seq(
    "institution_id",
    "name",
    "address",
    "phone",
    "fax",
    "email",
    "internet"
  )
  override val resourceTyppeForUrlQuery: String = "institution"
}

// WARNING: the crawledLanguage parameter is not used at the moment.
// At the moment, the crawler only fetches and extracts the English version of the Gepris website.
case class CrawledResourceData(resourceType: String, resourceId: String, crawledLanguage: String, html: String)

case class ReviewBoard(name: String, subjectAreas: Seq[String])
case class ResearchArea(name: String, reviewBoards: Seq[ReviewBoard])