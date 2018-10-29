package gepriscrawler.stage2.extractpersons.personextractor

import gepriscrawler.helpers.ExtractorHelpers
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.specs2._

import scala.collection.JavaConverters._


class PersonExtractorTest extends Specification {

  val html =
    """
      |<div class="content_frame">
      |  <div>
      |    <span class="name">
      |            Sprecherin
      |          </span>
      |
      |    <span class="value">
      |
      |            <a class="intern" href="/gepris/person/173975800">Professorin Dr. Dorothee  Brantz</a>
      |                        <br>                          Technische Universität Berlin <br>Institut für Kunstwissenschaft und Historische Urbanistik <br>Center for Metropolitan Studies<br> |                                                                                                                                                        Hardenbergstraße 16-18 |                    <br> |                                                                        10623 Berlin |                    <br> |                                                                                                Telefon: +49 30 31428406 |                  <br> |                                                                                                                                                                                    E-Mail: dorothee.brantz<img src="/gepris/images/at_symbol.png" alt="@">metropolitanstudies.de |                <br>
      |                                                                  </span><!-- value -->
      |  </div>
      |
      |  <div>
      |    <span class="name">
      |            aus­län­di­sche Spre­che­rin­nen / aus­län­di­sche Spre­cher
      |          </span>
      |
      |    <span class="value">
      |                                                                              <a class="intern" href="/gepris/person/34261821">Professor Dr. Roger  Keil</a>;
      |                                                                              <a class="intern" href="/gepris/person/173992476">Professorin Dr. Rosemary  Wakeman</a>
      |                </span><!-- value -->
      |  </div>
      |
      |</div>
    """.stripMargin

  val doc: Document = Jsoup.parse(html)

  val contentFrame = doc.select("div.content_frame")
  val allNameFields: Seq[Element] = contentFrame.select("span.name").asScala


  def is = s2"""
                                                               | The ProjectExtractorGraph.extractPersonIdByRegex method, applied to the given test HTML
                                                               | should match exactly the person with id '173975800' for 'Sprecherin' $speaker
                                                               | should match exactly the person with id '173975800' for 'Sprecherin' $foreignSpeaker
  """

  def speaker = {
    val spokepersonsIds = ExtractorHelpers.extractResourceIdsFromLinkByResourceTypeAndRegex(allNameFields)("person")(Seq("Sprecher(in){0,1}"))
    spokepersonsIds must beEqualTo(List("173975800"))
  }

  def foreignSpeaker = {
    val foreignSpokepersonsIds = ExtractorHelpers.extractResourceIdsFromLinkByResourceTypeAndRegex(allNameFields)("person")(Seq("ausländischer{0,1} Sprecher.*"))
    foreignSpokepersonsIds must beEqualTo(List("34261821", "173992476"))
  }
}
