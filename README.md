# Gepris Crawler

*Hinweis: Angaben zu gemachten Beobachtungen und Berechnungen auf Grundlage der gecrawlten Gepris-Daten beziehen sich auf den Stand der Gepris-Website vom 20.10.2018*


Der Gepris Crawler ist eine in Scala geschriebene Konsolenanwendung, welche die von der DFG bereitgestellten Förderungsdaten (https://gepris.dfg.de) crawled, extrahiert und in einem Ordner in Form von CSV- und Text-Dateien zur Verfügung stellt. 

Die Software entstand im Rahmen der Anfertigung der Bachelorarbeit von Daniel Spaude im Fach Informatik an der Freien Universität Berlin im Jahr 2018. 

## Datenqualität
Bei der Entwicklung wurde ein wesentlicher Fokus auf die Datenqualität gelegt. 
Es wurden dabei Datenqualitätskriterien definiert, welche grundsätzlich relevant für die Anwendung erschienen. 

Diverse dieser Kriterien wurden einerseits bei der Entwicklung des Crawlers besonders beachtet um deren Anforderungen möglichst einzuhalten. 

Andererseits wurden für diese ausgewählten Kriterien R-Skripte geschrieben, welche den tatsächlichen Gerad der Einhaltung dieser Kriterien für das Ergebnis eines Crawling-Durchlaufes bestimmen. 
Diese R-Skripte befinden sich gesammelt in dem Markdown-basierten R-Notebook `dataquality-evaluation.Rmd`. 


## Voraussetzungen
* Scala (2.12.6 oder höher)
* SBT (1.1.5 oder höher)
* Docker (empfohlen)

Wenn keine Anpassungen am Quelltext vorgenommen werden sollen und die Anwendung lediglich ausgeführt werden soll, reicht ein installiertes Docker und ein entsprechendes Docker-Images welches den Crawler beinhaltet. 

## Starten der Anwendung

### Starten über Docker
Der Crawler verwendet [Docker](https://www.docker.com/) als Container-Lösung, um eine möglichst einfache Inbetriebnahme zu ermöglichen. 
Sollte ein bereits gebautes Docker-Image für die Anwendung vorliegen (bereitgestellt beispielsweise über ein Docker-Repository wie https://hub.docker.com/r/spaudanjo/gepriscrawler), ist dann ein installiertes Docker die einzige Voraussetzung zum Ausführen des Crawlers.  
Dieser kann dann über den folgenden Befehl gestartet werden:

```docker run --rm -v ~/gepris-crawls:/crawls spaudanjo/gepriscrawler:0.3 CRAWLER-BEFEHL```

In diesem Beispiel ist ```gepriscrawler``` der Name des Docker-Images und 0.3 die Version des Images, wobei der Crawler-Befehl ```CRAWLER-BEFEHL``` aufgerufen wird und ```~/gepris-crawls``` das Verzeichnis auf dem Host-System ist, in welchem die Crawling-Ergebnisse abgespeichert werden sollen. Es wird innerhalb des Docker-Systems als ```/gepris-crawls``` eingebunden und kann dort von dem Crawling-Prozess genutzt werden. 


### Starten über SBT

Es handelt sich bei dem Crawler um eine [SBT](https://www.scala-sbt.org/)-basierte Scala-Anwendung. 
SBT ist ein Build-Tool, welches das Konfigurieren und Ausführen von Aufgaben wie das Ausführen von Tests oder das Kompilieren und Packaging einer Anwendung ermöglicht. 

Um den Crawler mittels SBT auf Grundlage des aktuellen Quellcodes zu kompilieren und auszuführen, kann folgender Befehl verwendet werden: 

```sbt run CRAWLER-BEFEHL```

### Crawler-Befehle

Unabhängig davon, ob die Anwendung über Docker oder direkt mittels SBT oder Java gestartet wird, stehen folgende Crawler-Befehle bereit: 

* ```--help``` - zeigt einen Hilfetext mit Erklärungen zum Starten des Crawlers an, ähnlich dem vorliegenden
* ```new-crawl ROOT_FOLDER``` - startet einen neuen Crawling-Vorgang und schreibt das Ergebnis in das Verzeichnis ```ROOT_FOLDER```
* ```resume-crawl FOLDER-OF-CRAWL``` - setzt einen nicht beendeten Crawling-Vorgang fort, welcher sich im Ordner ```FOLDER-OF-CRAWL``` befindet

### Probleme während der Ausführung
Es kann vorkommen, dass der Crawler vorzeitig terminiert, zum Beispiel weil das von der Gepris-Anwendung verwendete Cookie invalide geworden ist. 
In diesem Fall kann der Crawling-Vorgang über den Befehl ```resume-crawl FOLDER-OF-CRAWL``` fortgesetzt werden. ```FOLDER-OF-CRAWL``` ist dabei der absolute Pfad des des Ordners, in welchen der unterbrochene Crawlingvorgang seine Zwischen- und Endresultate geschrieben hat. 

## Erstellen der lauffähigen Crawler-Anwendung
### Erstellen als jar-Datei
Über den Befehl ```sbt package``` wird eine ausführbare jar-Datei angelegt. Dafür werden, falls keine aktuellen Kompilate vorliegen, die Scala-Quelldateien im Vorfeld zu class-Dateien in Java-Bytecode kompiliert. 
Dieses jar-Datei kann dann mittels Java ausgeführt werden, wobei Scala auf dem System installiert sein muss. 

### Erstellen eines Docker-Images
Es ist auch möglich mittels eines einfachen Befehls die Anwendung als Docker-Image bereitzustellen: 
```sbt docker:publishLocal```
Dieser Befehl kompiliert und packt die Anwendung und erstellt dann das Docker-Image, welches dann wie oben beschrieben ausgeführt werden kann. 



## Grundlegendes über die Domäne 'Gepris'

Bevor die auf die Ausgabeartefakte des Crawlers und damit auf die bereitgetellten Informationen und auf deren Organisation beschrieben wird, soll auf das Gepris-System an sich eingegangen werden und insbesondere auf die drei zentralen Resourcentypen Projekte, Institutionen und Personen, sowie auf die Fachsystematik. 

Gepris steht für "Geförderte Projekte der DFG". Die DFG (Deutsche Forschungsgemeinschaft) stellt über die [Gepris-Webanwendung](https://gepris.dfg.de/gepris/OCTOPUS) umfangreiche Informationen zu den von ihnen gemachten Förderungsaktivitäten bereit. 
Im Vordergrund stehen dabei vor allem die geförderten Projekte an sich, sowie die damit in Verbindung stehenden Institutionen und Personen. 

Neben einer Freitextsuche lassen sich alle Entitäten dieser drei Resourcentypen durch ein [Katalogsystem](https://gepris.dfg.de/gepris/OCTOPUS?task=showKatalog) auflisten, wobei 
die jeweiligen Detailseiten zu jeder Entiät direkt verlinkt sind. 
Ein Beispiel für eine Detailseite zum Resourcentyp 'Projekt' ist unter folgender URL zu finden: 
https://gepris.dfg.de/gepris/projekt/268853?language=de

### Personen und Institutionen 
Beide Ressourcen sind sehr einfach gehalten und selbsterklärend, sie enthalten Namen, Adressdaten und eine Liste von Projekte, zu welchen eine Beziehung besteht. 

### Projekte
Im Fokus der Domäne Gepris stehen Projekte. Sie sind das zentrale Konzept der Förderungsaktivitäten. 

Im folgenden sollen die Felder dieser Resource beschrieben werden: 

#### Titel/Name
Beispiel: 'GRK 6:  Räumliche Statistik'

#### Fachliche Zuordnung (Subject Area) - die DFG-Fachsystematik
Beispiel: 'Mathematik'

Die DFG organisiert sich bezüglich der fachlichen Zuordnung von Projekten bei ihren Förderungsaktivitäten in einer vierstufigen Systematik, welche näher unter folgenden beiden URLs beschrieben ist und auf deren Stufen hier kurz eingegangen werden soll: 
* https://www.dfg.de/en/dfg_profile/statutory_bodies/review_boards/subject_areas/index.jsp

* https://www.dfg.de/download/pdf/dfg_im_profil/gremien/fachkollegien/amtsperiode_2016_2019/fachsystematik_2016-2019_de_grafik.pdf

##### 1. Stufe: Wissenschaftsbereiche (Scientific Discipline) 
Auf der obersten Ebene stehen dabei die Wissenschaftsbereiche (Scientific Discipline), wo von es insgesamt vier Stück gibt: 
* Geistes- und Sozialwissenschaften
* Lebenswissenschaften
* Naturwissenschaften 
* Ingenieurwissenschaften


##### 2. Stufe: Fachgebiete (Research Area)
Die Fachgebiete sind die nächste Stufe, diese sind unter TODO DEM OBIGEN PDF dokumentiert: 

* Agrar-, Forstwissenschaften und Tiermedizin
* Bauwesen und Architektur
* Biologie
* Chemie
* Geisteswissenschaften
* Geowissenschaften
* Informatik, System- und Elektrotechnik
* Maschinenbau und Produktionstechnik
* Materialwissenschaft und Werkstofftechnik
* Mathematik
* Medizin
* Physik
* Sozial- und Verhaltenswissenschaften
* Wärmetechnik/Verfahrenstechnik

##### 3. Stufe: Fachkollegium (Review Board)
Dies stellt die vorletzte Ebene dar, welche die einzelnen Fächer (Subject Areas) gruppiert. Es gibt insgesamt 48 Fachkollegien. 
Beispiele hierfür sind 'Geschichtswissenschaften', 'Zoologie', 'Wasserforschung' oder 'Informatik'. 

##### 4. Stufe: Fach (Subject Area)
Auf der untersten Stufe sind schlussendlich die einzelnen Fächer aufgelistet. Es gibt insgesamt 213 Fächer und Beispiele sind 'Softwaretechnik und Programmiersprachen', 'Physische Geographie', 'Systemische Neurowissenschaft, Computational Neuroscience, Verhalten' oder 'Theater- und Medienwissenschaften'. 


Auf eine festgestellte Diskrepanz zwischen dieser offiziellen, von der DFG so dargestellten Systematik, und jener des Feldes `Fachgebiet` auf der [Katalog-Maske für Projekte im Gepris-System](https://gepris.dfg.de/gepris/OCTOPUS?task=doKatalog&context=projekt), wird im Abschnitt 'Inkonsistenzen hinsichtlich der Fachsystematik' eingegangen. 

#### Förderung (Term)
Dieses Feld informiert über den Förderungszeitraum des jeweiligen Projektes. 
Leider ist dieses nicht 'normalisiert' in Form zweier getrennter Felder 'Startjahr der Förderung' und 'Endjahr der Förderung' bereitgestellt, was jedoch aus Usability-Sicht Sinn macht. 

Mittels eines R-Skriptes und auf Grundlage der Datei 'generic_field_extractions.csv' wurden dabei folgende fünft Schemata für die Wertbelegung des Feldes identifiziert (hier im Kontext der englischsprachigen Gepris-Version und wobei XXXX für eine Jahreszahl steht): 

1. from XXXX to XXXX
2. since XXXX<br>
3. Funded in XXXX
4. until XXXX<br>
5. Currently being funded.

#### Projektkennung (Project identifier)
Jedes Projekt verfüg über eine eindeutige Id. 

#### Projektbeschreibung (Project Description)
Eine Zusammenfassung des Forschungsvorhabens in Freitext. 

#### DFG-Verfahren (DFG Programme)
Die DFG bietet verschiedene Förderprogramme an, welche sich hinsichtlich personellem Umfang, Laufzeit, Anforderungen an den Antragsteller und anderen Aspekten unterscheiden. Eine Übersicht über die verschiedenen Verfahrenstypen findet sich unter https://www.dfg.de/en/research_funding/programmes/index.html. 

Im Falle der DFG-Verfahren wird eine dreistufige Organisation verwendet, aufgeteilt in die folgenden Ebenen: 
* Programmgruppe (Programme Group) - Beispielsweise 'Einzelförderungen'
* Programm (Programme) - Beispielsweise 'Heisenberg-Programm'
* Programmlinie (Programme line) - Beispielsweise 'Heisenberg-Professuren'

Insgesamt wurden im aktuellen Crawl 35 Programmlinien gefunden. 

#### Weitere Felder 
Die bisher vorgestellten Felder machen den mit Abstand größten Anteil aus, sie sind auf fast jeder Projektseite angegeben. 
Tatsächlich kommen die Felder DFG-Verfahren, Projektbeschreibung, Projektkennung und Förderung (der Förderungszeitraum) bei ausnahmlos allen 116261 gecrawlten Projekten vor, dicht gefolgt von der 'Fachlichen Zuordnung' mit 115556 Fällen, also bei mehr als 99% der Projektseiten. 

Weitere Felder, die jedoch nicht auf allen Projektseiten vertreten sind, sind: 

* Website: Ein Link, welcher auf die Projektseite, meist bei der antragstellenden Institution, verweist

* 'Unterprojekt von' ('Subproject of'): Einige DFG-Programme sind Cluster-Programme, deren Projektausprägungen also als Elternprojekte für Unterprojekte dienen. 

* 'Internationaler Bezug' ('International connection'): In einigen Fällen sind unter diesem Feld Länder aufgelistet, welche Bezug zu dem jeweiligen Projekt aufweisen. 

* 'Beteiligte Fachrichtungen' ('Participating subject areas'): Bei einigen Projekten, insbesondere jene welche einen hohen Grad an Inolviertheit von mehreren Personen und Institutionen wie z.B. divere Exzellenzcluster aufweisen, sind neben dem Feld 'Fachliche Zuordnung' unter dem Feld 'Beteiligte Fachrichtungen' weitere Nebendisziplinen aufgeführt (tatsächlich kommt das Feld nur in folgenden DFG-Verfahren vor: Collaborative Research Centres, CRC/Transregios, Research Training Groups, CRC/Transfer Units, CRC/Cultural Studies Research Centres, International Research Training Groups, DFG Research Centres, Graduate Schools, Clusters of Excellence, Research Grants)


### Inkonsistenzen hinsichtlich der Fachsystematik

Die beschriebene dreistufige Fachsystematik stelt eine elegante Orientierungshilfe hinsichtlich der über 200 von der DFG aufgelisteten Fächer dar. 

Wie bereits beschrieben scheint das Gepris-System hier jedoch leider abzuweichen und ist damit nicht konsistent mit der offiziellen DFG-Fachsystematik. 

Es lassen sich aktuell nur ca. die Hälfte aller auf Projekten angegebenen Fächer in der offiziellen Fachsystematik wiederfinden. 


## Ausgabe-Artefakte des Crawlers

Der Crawler legt für einen neuen Crawlingvorgang einen eigenen Ordner an, benannt anhand des aktuellen Datums, um bei mehreren Crawling-Vorgängen später leichter die Übersicht zu behalten. 

Bezüglich der erzeugten Endresultate des Crawlers, sind zwei Ordner relevant: 

* ```final``` - hier liegen die primären Ergebnisse in Form von CSV- und Text-Dateien 
* ```stage1``` - hier liegen, jeweils in einem eigenen Unterordner für jeden Gepris-Resourcentyp (Projekt, Institution, Person), die HTML-Dateien, welche die Anwendung während des Crawlings von der Gepris-Website geladen hat und auf deren Basis das Extracting durchgeführt wurde

In den meisten Fällen wird sich das Interesse auf die Dateien innerhalb des Ordnes ```final``` konzentrieren. 

In manchen Fällen besteht aber auch Interesse an den HTML-Dateien, zum Beispiel: 
* zur Überprüfung der korrekten Arbeitsweise des Crawlers
* falls Interesse an einem nachträglichen Extracting eines Feldes besteht, welcher von der Extractor-Logik des Crawlers nicht beachtet wurde
* um einen Eindruck zu erhalten, wie die Gepris-Webseite einer bestimmten Ressource zum Zeitpunkt des Crawlings aussah


## Inhalt des Ordners 'final'

Der Ordnerinhalt lässt sich leicht in vier grundlegende Kategorien einteilen: 

### Anzahl der vom Gepris-System bereitgestellten Ressourcen

Auf oberster Ebene beinhaltet der Ordner die drei Dateien
* number_of_institutions_in_gepris_system.txt
* number_of_persons_in_gepris_system.txt
* number_of_projects_in_gepris_system.txt

Diese geben pro Resourcentyp die Anzahl der zum Zeitpunkt des Crawling auf der Gepris-Webseite verfügbaren Entitäten an. 
Die Information wird dabei aus der Navigationleise auf der Katalogseite des jeweiligen Resourcentyps ermittelt, [hier beispielhaft für den Resourcentyp 'Projekte'](https://gepris.dfg.de/gepris/OCTOPUS?task=doKatalog&context=projekt&oldfachgebiet=%23&fachgebiet=%23&nurProjekteMitAB=false&bundesland=DEU%23&oldpeo=%23&peo=%23&zk_transferprojekt=false&teilprojekte=false&teilprojekte=true&bewilligungsStatus=&beginOfFunding=&gefoerdertIn=&oldGgsHunderter=0&ggsHunderter=0&einrichtungsart=-1&findButton=Finden).


### Extraktion aller gefundenen Feldwerte für alle Resourcen

Mittels des generischen CSS-Selektors 

```#detailseite > div > div > div.content_frame > div.detailed .name```

kann der Crawler die allermeisten Informationen zu jedem Ressourcentyp in Form von Name-Wert-Tupeln aus den Detailseiten extrahieren. 
Diese sind in der Datei 

```csv/generic_field_extractions.csv``` 

abgespeichert, welche die Spalten resource_type, resource_id, field_name, field_value und beinhaltet. 

Ein Beispiel für einen Eintrag ist: 

```"project",5410165,"Term","from 2003 to 2008"```

Es handelt sich also um das Feld 'Term' des Projektes mit der Id '5410165', welches den Wert 'from 2003 to 2008' hat. 

Anhand dieses Beispieles wird ein Nachteil dieses generischen Ansatzes deutlich: 
die Informationen des Feldes 'Term', also über den Förderungszeitraum des Projektes, werden nicht strukturiert aufgeschlüsselt nach Start- und Endjahr, sondern als natürlichsprachliche Ellipse in Form einer Zeichenkette bereitgestellt. Dies erschwert die weitergehende Analyse. 

### Resourcentyp-spezifische und normalisierte Extraktion

Das eben beschrieben Problem wird durch die Extractor-Logik des Crawlers gelöst, welcher Resourcentyp-spezifische CSS-Selektoren, reguläre Ausdrücke und in manchen Fällen Transformationen auf den Feldwerten anwendet. 
Das Ergebnis sind Resourcentyp-spezifische CSV-Dateien, welche dem Normalisierungsideal aus dem Gebiet der relationalen Datenbanken nahe kommen. 

Für Daten, in deren Zentrum der Resourcentyp "Projekt" steht, stellt der Crawler folgende CSV-Dateien bereit: 

* ```project/extracted_project_data.csv``` - stellt allgemeine Kerninformationen zu Projekten dar und hat konkret folgende Spalten: 
  * project_id - Die DFG-Projektkennung 
  * title - Der Name des Projektes
  * project_description - Die Projektbeschreibung
  * dfg_programme - der Förderungstyp 
  * funding_start_year
  * funding_end_year
  * parent_project_id

  * Darüber hinaus gibt es eine Reihe von Beziehungstabellen, welche project_ids z.B. mit subject_areas, participating_subject_areas, Länder (über die Tabelle projects_international_connections.csv) sowie Personen und Institutionen in Verbinung setzt

Die Spalten der Tabellen ```person/extracted_person_data.csv``` sowie ```institution/extracted_Institution_data.csv``` sind alle selbsterklärend. 






## R-Markdown-Notebook zur Auswertung der Datenqualität

Die Datei ```dataquality-checks.Rmd``` kann zum Beispiels mittels der kostenlosen Software RStudio ausgeführt werden. 
Dazu, damit die relativen Pfade innerhalb des R-Skripts korrekt funktionieren, sollte sie im Ausgabeordner eines Crawling-Durchlaufes liegen, den es auszuwerten gibt.
Sie erzeugt je nach gewähltem Ausgabeformat eine HTML- oder PDF-Datei mit den. 