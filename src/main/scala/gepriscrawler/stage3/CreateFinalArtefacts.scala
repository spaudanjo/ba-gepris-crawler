package gepriscrawler.stage2

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import gepriscrawler.GeprisResources
import org.apache.commons.io.FileUtils
import org.zeroturnaround.zip.ZipUtil


object CreateFinalArtefacts {

  private def copyResourceToFolder(resourcePath: String, destinationPath: String) = {
    println(s"resourceName: $resourcePath")
    println(s"destinationPath: $destinationPath")
    val srcPathUrl = getClass().getResource(resourcePath).getPath
    val srcDir = new File(srcPathUrl)
    val destDir = new File(destinationPath)
    FileUtils.copyDirectory(srcDir, destDir)
  }

  def packHtmlToZipFor(exportRootPath: Path, resourceName: String) =
    FileUtils.copyDirectory(
      new File(Paths.get(exportRootPath.toString, "stage1", resourceName, "html").toString),
      new File(Paths.get(exportRootPath.toString, "final" , "html", resourceName).toString)
    )
//    ZipUtil.pack(
//    new File(Paths.get(exportRootPath.toString, "stage1", resourceName, "html").toString),
//    new File(Paths.get(exportRootPath.toString, "final", "html", s"$resourceName.zip").toString)
//  )

  def copyNumberOfResourcesTxtFilesToFinalFolder(exportRootPath: Path) = {
    GeprisResources.resourceList
      .map(_._1)
      .foreach(resourceType =>
        Files.copy(
          Paths.get(exportRootPath.toString, "stage0", resourceType, s"number_of_${resourceType}s_to_crawl.txt"),
          Paths.get(exportRootPath.toString, "final", s"number_of_${resourceType}s_in_gepris_system.txt"),
          StandardCopyOption.REPLACE_EXISTING
        )
      )
  }

  def copyCsvToFinalFolder(exportRootPath: Path) = {

    // Create sub folders
    (new File(Paths.get(exportRootPath.toString, "final", "csv").toString)).mkdirs()
//    (new File(Paths.get(exportRootPath.toString, "final", "html").toString)).mkdirs()

    // copy subject_areas.csv from stage0
    Files.copy(
      Paths.get(exportRootPath.toString, "stage0", "subject_areas.csv"),
      Paths.get(exportRootPath.toString, "final", "csv", "subject_areas.csv"),
      StandardCopyOption.REPLACE_EXISTING
    )

    // copy all folders and csv files from stage2
    FileUtils.copyDirectory(
      new File(Paths.get(exportRootPath.toString, "stage2").toString),
      new File(Paths.get(exportRootPath.toString, "final" , "csv").toString)
    )
  }

  // compress html folders from stage1 and move them to the corresponding folders in final
//  def packHtmlFilesForResourcesToZip(exportRootPath: Path) = GeprisResources.resourceList.map(_._1).foreach(packHtmlToZipFor(exportRootPath, _))
//
//  def copyDataQualityScriptsToFinalFolder(exportRootPath: Path) = {
//    //    val pathToReademesAndRScripts = "/readmes-and-r-scripts"
//    //    println(s"path: $srcPathUrl")
//    val itemsToCopy = Seq("dataquality-checks")
//
//    itemsToCopy
////      // Filter out all hidden files (like .Rhistory etc)
////      .filter(!_.startsWith("."))
//      .foreach { itemToCopy =>
//      val srcPath = s"/readmes-and-r-scripts/${itemToCopy}"
//      val destPath = Paths.get(exportRootPath.toString, "final", itemToCopy).toString
//      copyResourceToFolder(srcPath, destPath)
//    }
//  }

//  def renameFinalFolder(exportRootPath: Path, crawlName: String) = {
//    // Rename final folder (to include the date/timestamp of the crawl
//    FileUtils.getFile(Paths.get(exportRootPath.toString, "final").toString).renameTo(
//      FileUtils.getFile(Paths.get(exportRootPath.toString, s"final-gepris-crawl-${crawlName}").toString)
//    )
//  }

}
