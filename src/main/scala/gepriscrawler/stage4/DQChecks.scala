package gepriscrawler.stage4

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Path, Paths}

import gepriscrawler.helpers.CrawlerHelpers
import gepriscrawler.stage4.DQCriterias.consistencyofrelationconstraints.ConsistencyOfRelationConstraintsCriteria

object DQChecks {

  case class DQCriteriaCheckResult(dataQualityMeasurement: Double, summary: String)
  trait DQCriteria {
    def name: String
    def description: String
    def reportFileName: String
    def executeCheck(csvPath: String): DQCriteriaCheckResult
  }

  val dqCriterias: Seq[DQCriteria] = Seq(ConsistencyOfRelationConstraintsCriteria)

  def check(exportRootPath: Path) = {
    val exportFinalPath = Paths.get(exportRootPath.toString, "final").toString
    val csvPath = Paths.get(exportFinalPath, "csv").toString
    val dqReportFolder = Paths.get(exportFinalPath, "dataquality-reports").toString
    (new File(dqReportFolder)).mkdirs()

    dqCriterias.foreach { dqCriteria =>
      val file = new File(Paths.get(dqReportFolder, dqCriteria.reportFileName).toString)
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(s"Critera name: ${dqCriteria.name}\n")
      bw.write(s"Criteria description: ${dqCriteria.description}\n")
      bw.write(s"Report date: ${CrawlerHelpers.currentDateTimeStr}\n")

      val dqCritCheckResult: DQCriteriaCheckResult = dqCriteria.executeCheck(csvPath)

      bw.write(s"Measurement: ${dqCritCheckResult.dataQualityMeasurement}\n")
      bw.write(s"Summary: ${dqCritCheckResult.summary}\n")
      bw.close()
    }
  }
}
