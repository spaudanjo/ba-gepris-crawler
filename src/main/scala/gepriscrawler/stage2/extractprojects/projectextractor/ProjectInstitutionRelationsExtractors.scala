package gepriscrawler.stage2.extractprojects.projectextractor

import gepriscrawler.helpers.ExtractorHelpers
import org.jsoup.nodes.Element


object ProjectInstitutionRelationsExtractors {

  def extractProjectInstitutionRelations(allNameFields: Seq[Element]) = {

    type ProjectInstitutionRelationType = String
    type FieldLabelVariations = Seq[String]

    def extractInstitutionIdsFromLinksByRegex = ExtractorHelpers.extractResourceIdsFromLinkByResourceTypeAndRegex(allNameFields)("institution")(_)

    val projectInstitutionRelationTypesToFieldLabelVariations: Seq[(ProjectInstitutionRelationType, FieldLabelVariations)] = Seq(
      "APPLYING_INSTITUTION" -> Seq("Applying institution"),
      "CO_APPLICANT_INSTITUTION" -> Seq("Co-applicant institution"),
      "FOREIGN_INSTITUTION" -> Seq("Foreign institution"),
      "PARTICIPATING_INSTITUTION" -> Seq("Participating institution", "Participating Institution"),
      "PARTICIPATING_UNIVERSITY" -> Seq("Participating university"),
      "PARTNER_ORGANISATION" -> Seq("Participating institution", "Participating Institution")
    )

    projectInstitutionRelationTypesToFieldLabelVariations
      .flatMap(singleProjectInstitutionRelationTypeToFieldLabelVariations =>
        extractInstitutionIdsFromLinksByRegex(singleProjectInstitutionRelationTypeToFieldLabelVariations._2)
          .map(institutionId => (institutionId -> singleProjectInstitutionRelationTypeToFieldLabelVariations._1))
      )
  }
}
