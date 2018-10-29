package gepriscrawler.stage3.sqliteexport

import java.io.File
import java.nio.file.{Path, Paths}

import com.github.tototoshi.csv.CSVReader
import scalikejdbc._


object SqliteAccess {
  def dumpExtractsToDb(exportRootPath: Path) = {
    val exportPath = Paths.get(exportRootPath.toString, "final")
    (new File(exportPath.toString)).mkdirs()

    val pathToDbFolder = s"${exportRootPath.toString}/final"
    implicit val sqliteDBSession = SqliteDBCreator.createDBAndSession(pathToDbFolder, "extracted_gepris_data.sqlite")
    SqliteDBCreator.createTables


    // Export persons CSV to DB
    val csvPathExtractedPersonsCsv: Path = Paths.get(exportRootPath.toString, "stage2", "person", "extracted_person_data.csv")
    val readerExtractedPersons: CSVReader = CSVReader.open(new File(csvPathExtractedPersonsCsv.toString))

    val personInsertTuples = readerExtractedPersons.allWithHeaders()
      .map { p => Seq(
        'personId -> p.get("person_id").get,
        'personName -> p.get("name").get,
        'institutionName -> p.get("institution_name"),
        'address -> p.get("address"),
        'phone -> p.get("phone"),
        'fax -> p.get("fax"),
        'email -> p.get("email"),
        'internet -> p.get("internet").get
      )}.toSeq
    sql"insert into persons (person_id, name, institution_name, address, phone, fax, email, internet) values ({personId}, {personName}, {institutionName}, {address}, {phone}, {fax}, {email}, {internet})"
      .batchByName(personInsertTuples:_*)
      .apply()


    // Export institution CSV to DB
    val csvPathExtractedInstitutions: Path = Paths.get(exportRootPath.toString, "stage2", "institution", "extracted_institution_data.csv")
    val readerExtractedInstitutions: CSVReader = CSVReader.open(new File(csvPathExtractedInstitutions.toString))
    val institutionInsertTuples = readerExtractedInstitutions.allWithHeaders()
      .map { p => Seq(
        'institutionId -> p.get("institution_id").get,
        'name -> p.get("name").get,
        'address -> p.get("address"),
        'phone -> p.get("phone"),
        'fax -> p.get("fax"),
        'email -> p.get("email"),
        'internet -> p.get("internet").get
      )}.toSeq
    sql"insert into institutions (institution_id, name, address, phone, fax, email, internet) values ({institutionId}, {name}, {address}, {phone}, {fax}, {email}, {internet})"
      .batchByName(institutionInsertTuples:_*)
      .apply()



    // Export projects CSV to DB
    val csvPathExtractedProjectsCsv: Path = Paths.get(exportRootPath.toString, "stage2", "project", "extracted_project_data.csv")
    val readerProjectIdsToCrawl: CSVReader = CSVReader.open(new File(csvPathExtractedProjectsCsv.toString))
    val projectsInsertTuples = readerProjectIdsToCrawl.allWithHeaders()
      .map { p => Seq(
      'projectId -> p.get("project_id").get,
      'title -> p.get("title").get,
      'projectDescription -> p.get("project_description").get,
      'dfgVerfahren -> p.get("dfg_programme").get,
      'fundingStartYear -> p.get("funding_start_year").get,
      'fundingEndYear -> p.get("funding_end_year").get,
      'parentProjectId -> p.get("parent_project_id").get
    )}.toSeq
    sql"insert into projects (project_id, title, project_description, dfg_programme, funding_start_year, funding_end_year, parent_project_id) values ({projectId}, {title}, {projectDescription}, {dfgVerfahren}, {fundingStartYear}, {fundingEndYear}, {parentProjectId})"
      .batchByName(projectsInsertTuples:_*)
      .apply()





    // Export person_projects CSV to DB
    val csvPathExtractedProjectInstitutionRelationsCsv: Path = Paths.get(exportRootPath.toString, "stage2", "project", "project_institution_relations.csv")
    val extractedProjectInstitutionRelationsC: CSVReader = CSVReader.open(new File(csvPathExtractedProjectInstitutionRelationsCsv.toString))
    val ProjectInstitutionRelationsInsertTuples = extractedProjectInstitutionRelationsC.allWithHeaders()
      .map { p => Seq(
        'projectId -> p.get("project_id").get,
        'institutionId-> p.get("institution_id").get,
        'relationType -> p.get("relation_type").get
      )}.toSeq
    sql"insert into institution_projects (project_id, institution_id, relation_type) values ({projectId}, {institutionId}, {relationType})"
      .batchByName(ProjectInstitutionRelationsInsertTuples:_*)
      .apply()





    // Export person_projects CSV to DB
    val csvPathExtractedPersonProjectsCsv: Path = Paths.get(exportRootPath.toString, "stage2", "project", "project_person_relations.csv")
    val readerExtractedPersonProjects: CSVReader = CSVReader.open(new File(csvPathExtractedPersonProjectsCsv.toString))
    val personProjectsInsertTuples = readerExtractedPersonProjects.allWithHeaders()
      .map { p => Seq(
        'projectId -> p.get("project_id").get,
        'personId-> p.get("person_id").get,
        'relationType -> p.get("relation_type").get
      )}.toSeq
    sql"insert into person_projects (project_id, person_id, relation_type) values ({projectId}, {personId}, {relationType})"
      .batchByName(personProjectsInsertTuples:_*)
      .apply()

    //    // at first, let's get the unique list of subject areas
    val csvPathProjectIdsToParticipatingSubjectAreas: Path = Paths.get(exportRootPath.toString, "stage2", "project", "project_ids_to_participating_subject_areas.csv")
    val readerProjectIdsToParticipatingSubjectAreas: CSVReader = CSVReader.open(new File(csvPathProjectIdsToParticipatingSubjectAreas.toString))
    val allProjectIdsToParticipatingSubjectAreasWithHeaders: Seq[Map[String, String]] = readerProjectIdsToParticipatingSubjectAreas.allWithHeaders()
    val distinctListOfParticipatingSubjectAreas: Seq[String] = allProjectIdsToParticipatingSubjectAreasWithHeaders.map(_.getOrElse("participating_subject_area", "")).distinct

    // insert each of them into the table participating_subject_areas and assign to them their new corresponding primary-key-id

    val participatingSubjectAreasWithIds: Map[String, Long] = distinctListOfParticipatingSubjectAreas.map { sA =>
      val newId = sql"insert into participating_subject_areas (name) values (${sA})"
        .updateAndReturnGeneratedKey.apply()
      (sA, newId)
    }.toMap

    allProjectIdsToParticipatingSubjectAreasWithHeaders.distinct.foreach { projectIdToParticipatingSubjectArea =>
      val projectId = projectIdToParticipatingSubjectArea.get("project_id").get
      val partSubAreaName = projectIdToParticipatingSubjectArea.get("subject_area").get
      val partSubAreaId: Long = participatingSubjectAreasWithIds.get(partSubAreaName).get
      sql"insert into projects_participating_subject_areas (project_id, participating_subject_area_id) values (${projectId}, ${partSubAreaId})"
        .update.apply()
    }


    // Export projects_subject_areas to DB
    val csvPathProjectIdsToSubjectAreas: Path = Paths.get(exportRootPath.toString, "stage2", "project", "project_ids_to_subject_areas.csv")
    val readerProjectIdsToSubjectAreas: CSVReader = CSVReader.open(new File(csvPathProjectIdsToSubjectAreas.toString))
    readerProjectIdsToSubjectAreas.all().distinct.foreach { line =>
      sql"insert into projects_subject_areas (project_id, subject_area_name) values (${line(0)}, ${line(1)})"
        .update.apply()
    }

    // Export subject_areas to DB
    val csvPathSubjectAreas: Path = Paths.get(exportRootPath.toString, "stage0", "subject_areas.csv")
    val readerSubjectAreas: CSVReader = CSVReader.open(new File(csvPathSubjectAreas.toString))
    readerSubjectAreas.all().distinct.foreach { line =>
      sql"insert into subject_areas_taxonomy (subject_area, review_board, research_area) values (${line(0)}, ${line(1)}, ${line(2)})"
        .update.apply()
    }

    // Export projects_international_connections to DB
    val csvPathProjectInternationlConnections: Path = Paths.get(exportRootPath.toString, "stage2", "project", "projects_international_connections.csv")
    val readerProjectInternationlConnections: CSVReader = CSVReader.open(new File(csvPathProjectInternationlConnections.toString))
    readerProjectInternationlConnections.all().distinct.foreach { line =>
      sql"insert into projects_international_connections (project_id, country) values (${line(0)}, ${line(1)})"
        .update.apply()
    }
  }

}
