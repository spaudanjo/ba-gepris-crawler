package gepriscrawler

import java.io.File

// Main entry point of the Crawler App
object App extends App {

  case class Config(
                     rootFolder: File = new File("."),
                     crawlToResumeFolder: File = new File("."),
                     stageLevelToStartFrom: Int = -1,
                     mode: String = "",
                     barPathStr: String = ""
                   )

  val parser = new scopt.OptionParser[Config]("gepriscrawler") {
    head("Gepris Crawler", "0.1")

    help("help").text("prints this usage text\n\n")

    cmd("foo-test").hidden().action((_, c) => c.copy(mode = "foo-test"))
    .children(
      arg[String]("barPathStr").action((x, c) =>
        c.copy(barPathStr = x)).text("The folder of the existing, incomplete crawl.\n")
    )

    cmd("new-crawl").action((_, c) => c.copy(mode = "new")).
      text("Start a new crawl").
      children(
        arg[File]("ROOT-FOLDER").action((x, c) =>
          c.copy(rootFolder = x)).text("The folder where the crawler will put it interim and output files into.\n")
      )

    cmd("resume-crawl").action((_, c) => c.copy(mode = "resume")).
      text("Resume an already started but aborted/failed crawl").
      children(
        arg[File]("FOLDER-OF-CRAWL").action((x, c) =>
          c.copy(crawlToResumeFolder = x)).text("The folder of the existing, incomplete crawl.\n"),
        opt[Int]('s', "stage-level").action( (x, c) =>
          c.copy(stageLevelToStartFrom = x) ).text("The stage level where you want to resume from (make sure that the " +
          "stage was actually already started in the former run, e.g. by checking wether the folder exists). Usually it's " +
          "better to skip this param and to let the crawler identify on its own where it should continue from.")

      )

    note(
      """
        |Example usage:
        |gepriscrawler start-new-crawl ~/gepris-crawls
        |
        |If you are using docker, then the bound folder between your Docker container and your host system
        |should match the ROOT-FOLDER, e.g.:
        |docker run --rm -v ~/gepris-crawls:/crawls gepriscrawler:0.1 start-new-crawl /crawls
      """.stripMargin)

  }

  parser.parse(args, Config()) match {

    case Some(config) =>
      config.mode match {
        case "new" => GeprisCrawler.startNewCrawl(config.rootFolder.getAbsolutePath())
        case "resume" => GeprisCrawler.resumeExistingCrawl(
          exportRootPath = config.crawlToResumeFolder.getAbsolutePath(),
          stageToStartFrom = (if(config.stageLevelToStartFrom == -1) None else Some(config.stageLevelToStartFrom))
        )
      }

      // If the user did't specify any command: show the info text about the usage of the Crawler App.
      if (config.mode.isEmpty) {
        parser.showUsage()
      }
    case None =>
    // arguments are bad, error message will have been displayed
  }

}





