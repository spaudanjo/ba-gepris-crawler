package gepriscrawler.helpers

import org.jsoup.nodes.Element
import scala.collection.JavaConversions._

object ExtractorHelpers {

  def extractResourceIdsFromLinkByResourceType(matchedHrefs: Iterable[String], resourceType: String) = {
    val resourceIdRegex = raw"""\/gepris\/$resourceType/(\d*)""".r

    val resourceIds = matchedHrefs.map { matchedHRefElement =>
      matchedHRefElement match {
        case resourceIdRegex(id) => id
        case _ =>  ""
      }
    }.filterNot(_ == "")

    resourceIds
  }

}
