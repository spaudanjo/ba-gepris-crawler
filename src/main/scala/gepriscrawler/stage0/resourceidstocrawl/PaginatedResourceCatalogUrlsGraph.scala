package gepriscrawler.stage0.resourceidstocrawl

import akka.NotUsed
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.{FlowShape, Graph}

object PaginatedResourceCatalogUrlsGraph {

  def graph(resourceName: String): Graph[FlowShape[Int, String], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val resourcesPerPage = 50
    val resourceNameForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceName).resourceTyppeForUrlQuery

    val numberOfResources: FlowShape[Int, Int] = b.add(Flow[Int])
    val numberOfPages: FlowShape[Int, Int] = b.add(Flow[Int].map(i => (i.toFloat / resourcesPerPage).ceil.toInt))

    // WARNING: languageCode is set statically to 'en' at the moment
    // Later on it's planned to support other languages (the Gepris is offering its data also in German at the moment).
    // Then, languageCode would be set dynamically.
    val languageCode = "en"
    val pageUrls: FlowShape[Int, String] = b.add(Flow[Int]
      .map(0 to _ - 1)
      .mapConcat { range =>
        range
          .map(_ * resourcesPerPage)
          .map(pageNumber => s"https://gepris.dfg.de/gepris/OCTOPUS?beginOfFunding=&bewilligungsStatus=&context=${resourceNameForUrlQuery}&continentId=%23&countryKey=%23%23%23&einrichtungsart=-1&fachlicheZuordnung=%23&findButton=historyCall&gefoerdertIn=&hitsPerPage=$resourcesPerPage&index=$pageNumber&null=All+Locations+%2F+Regions&nurProjekteMitAB=false&oldContinentId=%23&oldCountryId=%23%23%23&oldSubContinentId=%23%23&subContinentId=%23%23&task=doSearchExtended&teilprojekte=true&zk_transferprojekt=false&language=$languageCode")
      }
    )
    numberOfResources ~> numberOfPages ~> pageUrls

    FlowShape(numberOfResources.in, pageUrls.out)
  }

}
