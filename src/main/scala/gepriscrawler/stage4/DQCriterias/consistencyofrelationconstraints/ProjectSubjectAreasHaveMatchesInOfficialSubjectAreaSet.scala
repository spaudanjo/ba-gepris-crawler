package gepriscrawler.stage4.DQCriterias.consistencyofrelationconstraints

import java.io.File
import java.nio.file.Paths

import com.github.tototoshi.csv.CSVReader

object ProjectSubjectAreasHaveMatchesInOfficialSubjectAreaSet extends RelationConstraint {
  def name: String = "ProjectSubjectAreasHaveMatchesInOfficialSubjectAreaSet"
  def description = "Subject Area of a project needs to have a corresponding entry in the official 'DFG Fachsystematik' subject_areas table"

  private def getAllCsvRowsFor(csvFilePath: String, csvFileName: String) = CSVReader.open(
    new File(Paths.get(csvFilePath, csvFileName).toString)
  )
    .allWithHeaders()

  private def getAllOfficialSubjectAreas(csvPath: String) = getAllCsvRowsFor(csvPath, "subject_areas.csv")
    .map(_("subject_area"))

  private def getProjectIdsToSubjectAreas(csvPath: String) = getAllCsvRowsFor(Paths.get(csvPath, "project").toString, "project_ids_to_subject_areas.csv")

  override def executeCheck(csvPath: String): RelationConstraintCheckResult = {
    val officialSubjectAreas: Seq[String] = getAllOfficialSubjectAreas(csvPath)

    val projectIdsToSubjectAreas: Seq[Map[String, String]] = getProjectIdsToSubjectAreas(csvPath)
    val projectIdsToSubjectAreasWithoutMatchInOfficialSubjectAreasTable = projectIdsToSubjectAreas.filter {projectIdToSubjectArea =>
      !officialSubjectAreas.exists{ officialSubjectArea =>
        officialSubjectArea == projectIdToSubjectArea("subject_area")
      }
    }

    val totalNoOfCheckCases = projectIdsToSubjectAreas.length
    val noOfFailureCases = projectIdsToSubjectAreasWithoutMatchInOfficialSubjectAreasTable.length
    val noOfSuccessCases = totalNoOfCheckCases-noOfFailureCases

    val failureCasesForSummaryString = projectIdsToSubjectAreasWithoutMatchInOfficialSubjectAreasTable.map { failureCase =>
      s"(${failureCase("project_id")}, ${failureCase("subject_area")})"
    }

    RelationConstraintCheckResult(noOfSuccessCases, noOfFailureCases,
      s"""
         |For the following tuples of project_id and subject_area from the table project_ids_to_subject_areas,
         |we found cases for which we couldn't find a matching entry in the
         | subject_areas table (which is based on the official DFG Fachsystematik website):
         | ${failureCasesForSummaryString.mkString("\n")}
       """.stripMargin)
  }
}
