package pl.edu.pw.elka.devicematcher.utils

import edu.stanford.nlp.ie.crf.CRFClassifier
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
  * Created by szymon on 28.12.16.
  */
object NLPUtils {

  /**
    * Pobiera string i znajduje nazwy wlasne.
    * Dla znalezionych podmienia spacje na podkreslenia.
    * Classifier jest na stronie:
    * http://nlp.stanford.edu/software/stanford-ner-2015-12-09.zip
    * - w wypakowanym folderze katalog classifiers
    * @param document string ze slowami
    * @return
    */
  def markNamedEntities(document: String): String = {
    val classifier = CRFClassifier.getClassifier("classifiers/english.all.3class.distsim.crf.ser.gz")
    var tmpDoc = document.toLowerCase
    val triples = classifier.classifyToCharacterOffsets(tmpDoc.toUpperCase)
    for (triple <- triples) {
      println(triple)
      val namedEntity = tmpDoc.substring(triple.second, triple.third)
      val markedNamedEntity = namedEntity.replace(' ', '_')
      println(markedNamedEntity)
      tmpDoc = tmpDoc.replaceFirst(namedEntity, markedNamedEntity)
    }
    tmpDoc
  }

  /**
    * wersja OpenNLP, aby zadziałała należy ściągnąć classifier ze strony
    * http://opennlp.sourceforge.net/models-1.5/
    */
  //  def markNamedEntities(document: String): String = {
//    val resourcesStream = NLPUtils.getClass.getResourceAsStream("/classifiers/en-ner-location.bin")
//    val nameFinder = new NameFinderME(new TokenNameFinderModel(resourcesStream))
//    var tmpDoc = document.toLowerCase
//    val tokens = document.toUpperCase.split(' ')
//    val spans = nameFinder.find(tokens)
//    for (span <- spans) {
//      val length = span.getEnd-span.getStart
//      val namedEntity = new StringBuilder(tokens(span.getStart).toLowerCase)
//      for (i <- span.getStart+1 to span.getEnd-1)
//        namedEntity.append(" "+tokens(i).toLowerCase)
//      val markedNamedEntity = namedEntity.toString().replace(' ', '_')
//      tmpDoc = tmpDoc.replaceFirst(namedEntity.toString(), markedNamedEntity)
//    }
//    tmpDoc
//  }
}
