package gepriscrawler.stage2

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.specs2._

import scala.collection.mutable.ArrayBuffer

class GenericFieldExtractorTest extends Specification {

  val personDetailPageHtml =
    """
      |        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
      |    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
      |<html xmlns="http://www.w3.org/1999/xhtml" lang="de-DE" xml:lang="de-DE">
      |<head>
      |    <meta http-equiv="content-type" content="text/xml; charset=UTF-8" />
      |	<meta http-equiv="X-UA-Compatible" content="IE=8" />
      |	<meta name="apple-itunes-app" content="app-id=1026720991">
      |	<script type="text/javascript" src="/gepris/js/jquery-1.8.3.js"></script>
      |	<script type="text/javascript" src="/gepris/js/jquery.jstree.js"></script>
      |	<script type="text/javascript" src="/gepris/js/send_preferences.js"></script>
      |	<script type="text/javascript" src="/gepris/js/jquery-ui.js"></script>
      |
      |					        <meta name="description" content="Professor Dr. Thomas  Ågotnes,  Universitetet i Bergen, Postboks 7800, 5020 Bergen, Norwegen">
      |	        <title>DFG - GEPRIS - Professor Dr. Thomas  Ågotnes</title>
      |
      |	<meta http-equiv="expires" content="0" />
      |	<meta http-equiv="cache-control" content="no-cache" />
      |	<meta http-equiv="pragma" content="no-cache" />
      |
      |	<link rel="stylesheet" type="text/css" href="/gepris/styles/style.css" />
      |
      |	<link rel="shortcut icon" href="/gepris/images/favicon.ico" type="image/ico" />
      |</head>
      |
      |<body>
      |	<a name="header"></a>
      |<div id="wrapAll">
      |	<div id="wrapper">
      |
      |		<div>
      |			<a href="#inhalt" class="skip">Go directly to content</a>
      |			<a href="#toolbar" class="skip">Go directly to font size and contrast</a>
      |		</div>
      |
      |		<div class="header">
      |			<div class="serviceWrap">
      |				<div class="serviceBox">
      |
      |				<h1 class="hidden">Servicenavigation</h1>
      |
      |				<ul class="extern">
      |										<li><a href="http://www.dfg.de" title="Link to DFG website" target="_blank">DFG Homepage</a></li>
      |				</ul>
      |
      |				<ul class="intern">
      |
      |
      |										<li><a href="/gepris/OCTOPUS?task=showContact" title="Disclaimer / Copyright">Disclaimer / Copyright</a>&nbsp;|</li>
      |										<li><a href="/gepris/OCTOPUS?task=showSearchHelp" title="Help">Help</a>
      |
      |					&nbsp;|</li>
      |										<li><a href="/gepris/OCTOPUS?task=showMonitor" title="Data Monitor">Data Monitor</a>&nbsp;</li>
      |
      |											<li style="background:none"><a href="/gepris/person/282670177?language=de" title="Language">Deutsch</a></li>
      |									</ul>
      |
      |				</div>
      |
      |								<div class="identity">
      |				<div class="logo">
      |					<img src="/gepris/images/dfg_logo.gif"	height="31" width="250"	alt="Logo: Deutsche Forschungsgemeinschaft (DFG) — to home page" />
      |				</div>
      |
      |
      |
      |								<div class="GEPRIS-logo">
      |					<a href="/gepris" class="bildLink" title="Logo: GEPRIS — to start page">
      |						<img src="/gepris/images/GEPRIS_Logo.png" alt="Logo: GEPRIS — to start page" />
      |					</a>
      |				</div>
      |					<div class="naviWrap">
      |						<div id="hauptNavigation">
      |						<h1 class="hidden">Hauptnavigation</h1>
      |
      |						<ul class="navigation">
      |
      |							<li>
      |								<div >
      |									<a href="/gepris/OCTOPUS?task=showSearchSimple">Search</a>
      |								</div>
      |							</li>
      |							<li>
      |								<div >
      |									<a href="/gepris/OCTOPUS?task=showKatalog">Catalogue</a>
      |								</div>
      |							</li>
      |							<li>
      |								<div >
      |									<a href="/gepris/OCTOPUS?task=browsePersonIndex">People Index</a>
      |								</div>
      |							</li>
      |							<li>
      |																<div >
      |									<a href="/gepris/OCTOPUS?task=browseOrtsindex">Location Index</a>
      |								</div>
      |							</li>
      |							<li>
      |								<div  class="last">
      |										<a href="/gepris/OCTOPUS?task=showAbout" id="link_about_gepris">
      |									    About GEPRIS
      |										</a>
      |								</div>
      |							</li>
      |						</ul>
      |						</div>
      |						<div class="hauptnaviLine"></div>
      |					</div>
      |
      |				</div>
      |			</div>
      |		</div>
      |
      |
      |		<div class="main"><!-- START div#main -->
      |			<a name="inhalt"></a>
      |	<div class="mainLeft" id="detailseite">
      |		<div class="contentWrap">
      |			<div class="content">
      |
      |
      |                					<h1> Project Details</h1>
      |		    		<div class="right">
      |		        		<p class="intern">
      |									                    	<a href="/gepris/OCTOPUS" class="intern">
      |									<b>Back to list of results</b>
      |		                        </a>
      |		                    						</p>
      |		    		</div>
      |
      |		    				            <div class="content_frame">
      |		                	<h2>Person
      |											<span id="icons">
      |
      |        <a rel="nofollow" href="/gepris/person/282670177?displayMode=print&amp;language=en" title="Open print view" target="_blank">
      |            <img src="/gepris/images/iconPrint.gif" alt="Print View"/>
      |        </a>
      |    </span>
      |		                	</h2>
      |		                	<div class="content_inside detailed">
      |								<h3>Professor Dr. Thomas  Ågotnes</h3>
      |
      |<div class="details">
      |    	<p>
      |		<span class="name">Address</span>
      |        <span style="display: inline-block;">
      |                        						University of Bergen<br />
      |				            	            		Postboks 7800<br />
      |            	            						5020 Bergen<br />
      |				            						Norwegen<br />
      |				                    </span>
      |    </p>
      |
      |    <p>
      |
      |        	        		    </p>
      |	</div>
      |
      |<div class="clear">&nbsp;</div>
      |
      |
      | 	<div id="PDFViewFix"> 		<ul id="tabnav">
      |			<li id="tabbutton1" class="selected">
      |				<a href="">Projects</a>
      |			</li>
      |		</ul>
      |	</div>
      |    <div class="content_frame">
      |
      |    	<script>
      |            $(document).ready(function(){
      |                  $("#projekteNachRolle").jstree({
      |                    "themes" : {
      |                        "theme" : "classic",
      |                        "dots" : false,
      |                        "icons" : false
      |                    },
      |                    "core" : {
      |                        "animation" : 100,
      |                        "html_titles" : true
      |                    },
      |                    "plugins" : [ "themes", "html_data"]
      |                });
      |            });
      |        </script>
      |
      |    	<div id="projekteNachRolle">
      |		     <ul>
      |				<li id="PKP" >
      |			<a class="print_nolink">As Cooperation partner</a>
      |	    	<ul>
      |	    			    			<li id="EIN">
      |                		<a class="print_nolink">Current projects</a>
      |
      |				    	<ul class="projectList help">
      |												        	<li>
      |					        		<div>
      |					        								                							        		<div>
      |					                		<a class="intern" href="/gepris/projekt/282669151">
      |												From Shared Evidence to Group Attitudes
      |											</a>
      |					                		(Research Grants)
      |					                							        			</div>
      |					        		</div>
      |						    	</li>
      |													</ul>
      |					</li>
      |							</ul>
      |		</li>
      |	</ul>		</div>
      |
      |		<div class="clear">&nbsp;</div>
      |	</div><!-- content_frame -->
      |		                    </div><!-- content_inside detailed -->
      |						<div class="clear">&nbsp;</div>
      |		        	</div><!-- content_frame -->
      |					<div class="contentFooter"></div>
      |			                </div><!-- content -->
      |        </div><!-- contentWrap -->
      |    </div><!-- mainLeft -->
      |
      |        	<h1 class="hidden noPrint">Additional Information</h1>
      |	<div class="context">
      |    	            <div class="info">
      |                            </div>
      |
      |    </div><!-- context -->
      |    			</div><!-- main -->
      |
      |		<div class="footerWrap">
      |	   		<div class="div_fibonacci"><img class="footerImg_1" src="/gepris/images/fibonacci_footer.gif" alt="" title=""/></div>
      |	   	</div>
      |		<div class="copyrightBox">
      |							<span class="left">&copy; 2018 <abbr title="Deutsche Forschungsgemeinschaft">DFG</abbr></span>
      |							<span class="serviceLinksUnten"><a href="/gepris/OCTOPUS?task=showContact" title="Disclaimer / Copyright">Disclaimer / Copyright</a> / <a href="http://www.dfg.de/en/service/privacy_policy/index.html" target="_blank" title="Privacy Policy and Data Protection Notice">Privacy Policy and Data Protection Notice</a></span>
      |		</div>
      |	</div><!-- wrapper -->
      |</div><!-- wrapAll -->
      |
      |<div class="toolBar">
      |	<a name="toolbar"></a>
      |	<h1 class="hidden">Textvergr&ouml;ßerung und Kontrastanpassung</h1>
      |		<div class="textSizeBox">
      |				<a rel="nofollow" href="/gepris/person/282670177?fontSize=0&amp;language=en" title="Text size small" class="small"></a>
      |				<a rel="nofollow" href="/gepris/person/282670177?fontSize=1&amp;language=en" title="Text size medium" class="medium"></a>
      |				<a rel="nofollow" href="/gepris/person/282670177?fontSize=2&amp;language=en" title="Text size large" class="large"></a>
      |	</div>
      |	<div class="contrastBox">
      |				<a rel="nofollow" href="/gepris/person/282670177?contrast=0&amp;language=en" title="Decrease contrast" class="contrastNormal"></a>
      |				<a rel="nofollow" href="/gepris/person/282670177?contrast=1&amp;language=en" title="Increase contrast" class="contrastSpecial"></a>
      |	</div>
      |</div>
      |
      |	<!-- START OF SmartSource Data Collector TAG -->
      |<!-- Copyright (c) 1996-2015 Webtrends Inc.  All rights reserved. -->
      |<!-- Version: 9.4.0 -->
      |<!-- Tag Builder Version: 4.1  -->
      |<!-- Created: 3/3/2015 3:53:50 PM -->
      |<script src="/gepris/js/webtrends.js" type="text/javascript"></script>
      |<!-- ----------------------------------------------------------------------------------- -->
      |<!-- Warning: The two script blocks below must remain inline. Moving them to an external -->
      |<!-- JavaScript include file can cause serious problems with cross-domain tracking.      -->
      |<!-- ----------------------------------------------------------------------------------- -->
      |<script type="text/javascript">
      |//<![CDATA[
      |var _tag=new WebTrends();
      |_tag.dcsGetId();
      |//]]>
      |</script>
      |<script type="text/javascript">
      |//<![CDATA[
      |_tag.dcsCustom=function(){
      |// Add custom parameters here.
      |//_tag.DCSext.param_name=param_value;
      |}
      |_tag.dcsCollect();
      |//]]>
      |</script>
      |<noscript>
      |<div><img alt="DCSIMG" id="DCSIMG" width="1" height="1" src="//sdc.dfg.de/dcsgy77fy000000gki88uzevi_5d2b/njs.gif?dcsuri=/nojavascript&amp;WT.js=No&amp;WT.tv=9.4.0&amp;dcssip=www.gepris.dfg.de"/></div>
      |</noscript>
      |<!-- END OF SmartSource Data Collector TAG -->
      |
      |</body>
      |</html>
    """.stripMargin

  val doc: Document = Jsoup.parse(personDetailPageHtml)
  val detailSection = doc.select("div.detailed")

  def is = s2"""
                                                               | The GenericFieldExtractorGraph.extractTypeAndProjectIdsFromProjectsTree method, applied to the given test HTML of the person with the id '282670177',
                                                               | should extract the only made project reference to the project with id '282669151' and as type 'As Cooperation partner' $extractionOfReferencedProjectsOnPersonPage
  """

  def extractionOfReferencedProjectsOnPersonPage = {
    val extractedTypeAndProjectIds: Seq[(String, String)] = GenericFieldExtractorGraph.extractTypeAndProjectIdsFromProjectsTree(detailSection, "person")
    println(s"JO: $extractedTypeAndProjectIds")

    extractedTypeAndProjectIds must containTheSameElementsAs(Seq(("As Cooperation partner", "282669151")))
  }
}