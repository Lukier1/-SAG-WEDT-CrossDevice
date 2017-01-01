package pl.edu.pw.elka.devicematcher.utils

import java.io.StringReader
import java.util

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.{CoreAnnotation, CoreAnnotations, CoreLabel, TaggedWord}
import edu.stanford.nlp.process.{CoreLabelTokenFactory, Morphology, PTBTokenizer}
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import edu.stanford.nlp.util.{CoreMap, Timing}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

/**
  * Created by szymon on 28.12.16.
  */
object NLPUtils {

  /**
    * przyimki potrzebne do otagowania POS
    */
  private val prep = Array("abroad", "across", "after", "ahead", "along", "aside", "away", "around", "back", "down", "forward", "in", "off", "on", "over", "out", "round", "together", "through", "up")
  private val particles = util.Arrays.asList(prep)

  /**
    * Inicjalizacja zbioru stopwords
    */
  private val stream = NLPUtils.getClass.getResourceAsStream("/txt/stopwords.txt")
  private var stopWords = new mutable.HashSet[String]
  for (line <- Source.fromInputStream(stream).getLines) {
    stopWords.add(line)
  }


  /**
    * Pobiera string i znajduje nazwy wlasne.
    * Zwraca liste: [{dokument z wycietymi NE}, {NE 0}, {NE 1}, ...]
    *
    * Classifier jest na stronie:
    * http://nlp.stanford.edu/software/stanford-ner-2015-12-09.zip
    * - w wypakowanym folderze katalog classifiers
    *
    * @param document string ze slowami
    * @return lista, na miejscu z indeksem 0 jest dokument z wycietymi nazwami wlasnymi,
    *         na kolejnych miejscach sa kolejne nazwy wlasne
    */
  def markNamedEntities(document: String): util.List[String] = {
    /*pobiera z resurces, znajduje "london city"*/
    //val classifier = CRFClassifier.getClassifier("classifiers/english.all.3class.distsim.crf.ser.gz")
    /*pobiera z jara z mavena, znajduje samo "london" bez "city"*/
    val classifier = CRFClassifier.getClassifier("edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz")
    var namedEntities = new util.LinkedList[String]
    var tmpDoc = document.toLowerCase()
    val triples = classifier.classifyToCharacterOffsets(tmpDoc.toUpperCase)
    for (triple <- triples) {
      val namedEntity = tmpDoc.substring(triple.second, triple.third)
      namedEntities.add(namedEntity.toLowerCase)
    }
    for (namedEntity <- namedEntities) {
      tmpDoc = tmpDoc.replaceFirst(namedEntity, "")
    }
    val res = new util.LinkedList[String]
    res.add(tmpDoc)
    res.addAll(namedEntities)
    res
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

  /**
    * Pobiera dokument, przeprowadza tokenizacje,
    * usuwa tokeny niezawierajace zadnej litery
    *
    * @param document string ze slowami
    * @return lista tokenow
    */
  def tokenize(document: String): util.List[String] = {
    val ptbt = new PTBTokenizer(new StringReader(document), new CoreLabelTokenFactory(), "")
    val tokens = new util.ArrayList[String]()
    while (ptbt.hasNext()) {
      val label = ptbt.next()
      tokens.add(label.toString())
    }
    tokens
  }

  /**
    * Usuwa tokeny ktorych wartosci sa w pliku stopwords.txt
    *
    * @param tokens tokeny po tokenizacji
    * @return Lista wejsciowa bez tokenow ktore naleza do stopwords
    */
  def deleteStopWordsStrings(tokens: util.List[String]): util.List[String] = {
    val tmpTokens = new util.LinkedList[String](tokens)
    for (token <- tokens) {
      if (stopWords.contains(token))
        tmpTokens.remove(token)
    }
    tmpTokens
  }

  /**
    * Usuwa tokeny ktroych lemma jest w pliku stopwords.txt
    *
    * @param tokens tokeny po tokenizacji, tagowaniu pos oraz lematyzacji
    * @return Lista wejsciowa bez tokenow ktorych lemma nalezy do stopwords
    */
  def deleteStopWords(tokens: util.List[CoreLabel]): util.List[CoreLabel] = {
    val tmpTokens = new util.LinkedList[CoreLabel](tokens)
    for (token <- tokens) {
      if (stopWords.contains(token.lemma())) {
        //        println("stopword")
        tmpTokens.remove(token)
      }
    }
    tmpTokens
  }

  /**
    * Dokonuje lematyzacji. Wynik lematyzacji oraz oryginalny token znajduje sie w strukturach CoreLabel
    *
    * @param tokens Tokeny po tokenizacji i tagowaniu POS zawarte w strukturach CoreLabel
    * @return Lista struktur
    */
  def lemmatize(tokens: util.List[CoreLabel]): util.List[CoreLabel] = {
    val morphology = new Morphology()
    for (token <- tokens) {
      val text = token.get(classOf[CoreAnnotations.TextAnnotation])
      val posTag = token.get(classOf[CoreAnnotations.PartOfSpeechAnnotation])
      addLemma(morphology, classOf[CoreAnnotations.LemmaAnnotation], token, text, posTag)
    }
    tokens
  }

  /**
    * Taguje czesi mowy. Wynik tagowania oraz oryginalny token znajduje sie w strukturach CoreLabel
    *
    * @param tokens Tokeny po tokenizacji
    * @return Lista struktur CoreLabel
    */
  def tagPartOfSpeech(tokens: util.List[String]): util.List[CoreLabel] = {
    val labels = new util.LinkedList[CoreLabel]
    for (token <- tokens) {
      val coreLabel = new CoreLabel()
      coreLabel.setWord(token)
      labels.add(coreLabel)
    }
    val pos = loadModel(System.getProperty("pos.model", MaxentTagger.DEFAULT_JAR_PATH), false)
    var tagged: util.List[TaggedWord] = new util.LinkedList[TaggedWord]()
    if (labels.size() <= Integer.MAX_VALUE) {
      try {
        tagged = pos.tagSentence(labels, false);
      } catch {
        case e: OutOfMemoryError =>
          println("WARNING: Tagging of sentence ran out of memory.")
        case e: Exception =>
          println(e)
      }
    }

    if (tagged != null) {
      val size = labels.size - 1
      for (i <- 0 to size) {
        labels.get(i).set(classOf[CoreAnnotations.PartOfSpeechAnnotation], tagged.get(i).tag());
      }
    } else {
      for (token <- labels) {
        token.set(classOf[CoreAnnotations.PartOfSpeechAnnotation], "X");
      }
    }
    labels
  }

  private def loadModel(loc: String, verbose: Boolean): MaxentTagger = {
    var timer: Timing = null
    if (verbose) {
      timer = new Timing();
      timer.doing("Loading POS Model [" + loc + ']');
    }
    val tagger = new MaxentTagger(loc);
    if (verbose) {
      timer.done();
    }
    tagger;
  }

  private def phrasalVerb(morpha: Morphology, word: String, tag: String): String = {
    // must be a verb and contain an underscore
    assert(word != null)
    assert(tag != null)
    if (!tag.startsWith("VB") || !word.contains("_")) return null
    // check whether the last part is a particle
    val verb = word.split("_")
    if (verb.length != 2) return null
    val particle = verb(1)
    if (particles.contains(particle)) {
      val base = verb(0)
      val lemma = morpha.lemma(base, tag)
      return lemma + '_' + particle
    }
    null
  }

  private def addLemma(morpha: Morphology, ann: Class[_ <: CoreAnnotation[String]],
                       map: CoreMap, word: String, tag: String) {
    if (!tag.isEmpty) {
      val pVerb = phrasalVerb(morpha, word, tag)
      if (pVerb == null) map.set(ann, morpha.lemma(word, tag))
      else map.set(ann, pVerb)
    }
    else map.set(ann, morpha.stem(word))
  }


}
