package gepriscrawler.helpers

import org.jsoup.nodes.Element
import scala.collection.JavaConversions._

object ExtractorHelpers {

  def extractMultivaluesByFieldNames(parentElement: Element, fieldNames: Seq[String]): Seq[String] =
    fieldNames.flatMap { fieldName =>
      parentElement
        .select(s"span.name:matches($fieldName) + span")
        .html()
        .split("<br>")
        .flatMap(_.split(", "))
        .map(_.trim)
        .filterNot(_.isEmpty)
    }.distinct


  def extractFundingDateRange(parentElement: Element) = {
    val fundingDateRange = parentElement
      .select("span.name:matches(Term) + span.value")
      .text()

    val fromToRegex = "^.*from ([0-9]+) to ([0-9]+).*$".r
    val sinceRegex = "^.*since ([0-9]+).*$".r
    val fundedInRegex = "^.*Funded in ([0-9]+).*$".r
    val untilRegex = "^.*until ([0-9]+).*$".r
    val FundingOngoing = "^.*Currently being funded.*$".r

    val (start, end) = fundingDateRange match {
      case fromToRegex(start, end) => (start, end)
      case sinceRegex(seit) => (seit, "")
      case fundedInRegex(in) => (in, in)
      case untilRegex(bis) => ("", bis)
      case FundingOngoing() => ("ongoing", "ongoing")
      case _ => ("", "")
    }

    (start, end)
  }


  def extractResourceIdsFromLinkByResourceTypeAndRegex(elements: Seq[Element])(resourceType: String)(regexes: Seq[String]) = {

    // Scala uses the $ symbol for String interpolation.
    // For escaping a dollar symbol as wanted part of the string, we can use $$
    val enrichedRegexes = regexes.map(r => s"^\\s*$r\\s*$$")

    enrichedRegexes.flatMap { regex =>

      val resourceIdRegex = raw"""\/gepris\/$resourceType/(\d*)""".r
      val matchedElements: Seq[Element] = elements.filter(s => s.text().matches(regex))

      val matchedHrefs = matchedElements
        .flatMap(
          _.nextElementSibling()
            .select("a")
            .eachAttr("href")
        )

      val resourceIds = matchedHrefs.map { matchedHRefElement =>
        matchedHRefElement match {
          case resourceIdRegex(id) => id
          case _ =>  ""
        }
      }.filterNot(_ == "")

      resourceIds
    }
  }

}
