package gepriscrawler.stage4.DQCriterias.consistencyofrelationconstraints

import gepriscrawler.stage4.DQChecks._

case class RelationConstraintCheckResult(noOfSuccessCases: Int, noOfFailureCases: Int, summary: String)

trait RelationConstraint {
  def name: String
  def description: String
  def executeCheck(csvPath: String): RelationConstraintCheckResult
}

object ConsistencyOfRelationConstraintsCriteria extends DQCriteria {
  def name: String = "Consistency regarding defined relation constraints"
  def description(): String = "FOFOFOFOF DESCRIPTION"
  def reportFileName: String = "consistency_of_relations.txt"

  private def relationConstraints: Seq[RelationConstraint] = Seq(ProjectSubjectAreasHaveMatchesInOfficialSubjectAreaSet)

  override def executeCheck(csvPath: String): DQCriteriaCheckResult = {
    val relationConstraintResults = relationConstraints.map {relCon =>
      val relConsCheckResult = relCon.executeCheck(csvPath)
      val totalNoOfCheckCases = relConsCheckResult.noOfFailureCases + relConsCheckResult.noOfSuccessCases
      val dataQualityMeasurement = relConsCheckResult.noOfSuccessCases.toFloat / totalNoOfCheckCases
      val summary =
        s"""
           |Relation constraint name: ${relCon.name}
           |Description: ${relCon.description}
           |Measurement: ${dataQualityMeasurement}
           |No of Success Cases: ${relConsCheckResult.noOfSuccessCases}
           |No of Failure Cases: ${relConsCheckResult.noOfFailureCases}
           |Summary: ${relConsCheckResult.summary}
         """.stripMargin

      (relConsCheckResult.noOfSuccessCases, relConsCheckResult.noOfFailureCases, summary)
    }

    val totalNoOfSuccessCases = relationConstraintResults.map(_._1).sum
    val totalNoOfFailureCases = relationConstraintResults.map(_._2).sum
    val totalNoOfCheckCases = totalNoOfFailureCases + totalNoOfSuccessCases
    val totalDataQualityMeasurement = totalNoOfSuccessCases.toFloat / totalNoOfCheckCases
    val summary = relationConstraintResults.map(_._3).mkString("===========================\n")

    DQCriteriaCheckResult(totalDataQualityMeasurement, summary)
  }
}
