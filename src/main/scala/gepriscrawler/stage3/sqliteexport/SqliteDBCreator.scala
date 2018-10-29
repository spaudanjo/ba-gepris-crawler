package gepriscrawler.stage3.sqliteexport

import java.text.SimpleDateFormat
import java.util.Calendar

import scalikejdbc._

object SqliteDBCreator {

  def currentDateTimeStr: String = {
    val now = Calendar.getInstance().getTime()
    val minuteFormat = new SimpleDateFormat("y-MM-dd--HH-mm-ss")
    minuteFormat.format(now)
  }

  def createDBAndSession(pathToDbFolder: String, dbName: String = s"$currentDateTimeStr.sqlite") = {
    val jdbcConnectionStr = s"jdbc:sqlite:${pathToDbFolder}/$dbName"

    // initialize JDBC driver & connection pool

    Class.forName("org.sqlite.JDBC")
    ConnectionPool.add("new", jdbcConnectionStr, null, null)
    implicit val session: NamedAutoSession = NamedAutoSession("new")
    createTables
    session
  }

  def createTables(implicit session: NamedAutoSession) = {
    sql"""
         |CREATE TABLE IF NOT EXISTS `projects` (
         |	`project_id`	TEXT,
         |	`title`	TEXT,
         |	`project_description`	TEXT,
         |	`dfg_programme`	TEXT,
         |	`parent_project_id`	TEXT,
         |	`funding_start_year`	TEXT,
         |	`funding_end_year`	TEXT
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `persons` (
         |	`person_id`	TEXT,
         |	`name`	TEXT,
         |	`institution_name`	TEXT,
         |	`address`	TEXT,
         |	`phone`	TEXT,
         |	`fax`	TEXT,
         |	`email`	TEXT,
         |	`internet`	TEXT
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `institutions` (
         |	`institution_id`	TEXT,
         |	`name`	TEXT,
         |	`address`	TEXT,
         |	`phone`	TEXT,
         |	`fax`	TEXT,
         |	`email`	TEXT,
         |	`internet`	TEXT
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `projects_participating_subject_areas` (
         |	`project_id`	STRING NOT NULL,
         |	`participating_subject_area_id`	INTEGER NOT NULL
         |)
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `projects_international_connections` (
         |	`project_id`	STRING NOT NULL,
         |	`country`	STRING NOT NULL
         |)
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `projects_subject_areas` (
         |	`project_id`	STRING NOT NULL,
         |	`subject_area_name`	STRING NOT NULL
         |)
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `person_projects` (
         |	`project_id`	STRING NOT NULL,
         |	`person_id`	STRING NOT NULL,
         |	`relation_type`	STRING NOT NULL
         |)
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `institution_projects` (
         |	`project_id`	STRING NOT NULL,
         |	`institution_id`	STRING NOT NULL,
         |	`relation_type`	STRING NOT NULL
         |)
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `participating_subject_areas` (
         |	`id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
         |	`name`	TEXT NOT NULL UNIQUE
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         |CREATE TABLE IF NOT EXISTS `subject_areas_taxonomy` (
         |	`subject_area`	TEXT NOT NULL,
         |	`review_board`	TEXT NOT NULL,
         |	`research_area`	TEXT NOT NULL
         |);
         """.stripMargin
      .execute.apply()


    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `subject_areas_taxonomy_idx` ON `subject_areas_taxonomy` (
         |	`subject_area`	ASC,
         |	`review_board`	ASC,
         |	`research_area`	ASC
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `projects_participating_subject_areas_idx` ON `projects_participating_subject_areas` (
         |	`project_id`	ASC,
         |	`participating_subject_area_id`	ASC
         |);
         """.stripMargin
      .execute.apply()


    sql"""
         |CREATE UNIQUE INDEX IF NOT EXISTS 'projects_subject_areas_idx' ON 'projects_subject_areas' (
         |	'project_id'	ASC,
         |	'subject_area_name'	ASC
         |);
         """.stripMargin
      .execute.apply()


    sql"""
         |CREATE UNIQUE INDEX IF NOT EXISTS `participating_subject_areas_idx` ON `participating_subject_areas` (
         |	`name`	ASC
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `id` ON `projects` (
         |	`project_id`
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `person_id` ON `persons` (
         |	`person_id`
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `institution_id` ON `institutions` (
         |	`institution_id`
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `person_projects_idx` ON `person_projects` (
         |	`project_id`,
         |	`person_id`,
         |	`relation_type`
         |);
         """.stripMargin
      .execute.apply()

    sql"""
         CREATE UNIQUE INDEX IF NOT EXISTS `institution_projects_idx` ON `institution_projects` (
         |	`project_id`,
         |	`institution_id`,
         |	`relation_type`
         |);
         """.stripMargin
      .execute.apply()
  }
}
