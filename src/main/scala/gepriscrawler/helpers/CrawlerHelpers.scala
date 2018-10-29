package gepriscrawler.helpers

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString
import com.github.tototoshi.csv.{CSVReader, CSVWriter, DefaultCSVFormat, QUOTE_NONNUMERIC}
import org.apache.commons.io.FileUtils

import scala.concurrent.{ExecutionContext, Future}

object CrawlerHelpers {

  type CSVRow = Seq[String]

  def deleteAndCreateStageFolderByStageNumber(exportPath: Path, stageNumber: Int, deleteExistingFolderBefore: Boolean = true) = {
    val exportPathForStage: Path = Paths.get(exportPath.toString, s"stage$stageNumber")
    if(deleteExistingFolderBefore) FileUtils.deleteDirectory(new File(exportPathForStage.toString))
    (new File(exportPathForStage.toString)).mkdirs()
  }

  def currentDateTimeStr: String = {
    val now = Calendar.getInstance().getTime()
    val minuteFormat = new SimpleDateFormat("y-MM-dd--HH-mm-ss")
    minuteFormat.format(now)
  }


  def getResourceIdsAlreadyCrawledFromCsv(resourceType: String, exportRootPath: Path) = {
    val csvPathResourceIdsToCrawl: Path = Paths.get(exportRootPath.toString, "stage1", resourceType, s"crawled_${resourceType}_ids.csv")
    val readerResourceIdsToCrawl = CSVReader.open(new File(csvPathResourceIdsToCrawl.toString))

    val alreadyCrawledResourceIds = readerResourceIdsToCrawl.all()
      // ignore the first header line
      .drop(1)
      // get the first and only column, which is the institution-id
      .map(_ (0))
      .toSet

    readerResourceIdsToCrawl.close()

    alreadyCrawledResourceIds
  }


  def createCsvFileWriterSink(exportPath: Path, name: String, csvHeader: CSVRow, append: Boolean = true)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext) ={
    implicit object MyFormat extends DefaultCSVFormat {
      override val quoting = QUOTE_NONNUMERIC
    }

    val filePath = s"$exportPath/$name.csv"
    val fileExisted = Files.exists(Paths.get(filePath))
    val file = new File(filePath)
    val csvWriter = CSVWriter.open(file, append)
    // If the file already existed, we don't need to write the header row once more
    if(!fileExisted) {
      csvWriter.writeRow(csvHeader)
    }
    Flow[CSVRow].watchTermination()((_, f) => f.foreach(_ => csvWriter.close()))
      .to(Sink.foreach(csvWriter.writeRow))
  }

  def createTextFileWriterSink(fileName: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(fileName))) (Keep.right)
}
