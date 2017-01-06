package pl.edu.pw.elka.devicematcher

import java.util

import scala.collection.JavaConversions._
import edu.stanford.nlp.ling.CoreLabel
import pl.edu.pw.elka.devicematcher.data.{Database, DeviceQueryDAO, DocumentDAO}
import pl.edu.pw.elka.devicematcher.actors.ActorsPrototype
import pl.edu.pw.elka.devicematcher.topicmodel.{Document, TopicModel}
import pl.edu.pw.elka.devicematcher.utils.{MetricsUtils, NLPUtils, WordnetUtils}

object DeviceMatcherApp extends App {

  override def main(args: Array[String]): Unit = {
    //        val iterator = DeviceQueryDAO.getDeviceQueriesByDevice(1)
    //        while(iterator.hasNext)
    //          println(iterator.next())
    //
    //        Database.client.close()
      ActorsPrototype.main()
    /**
      * przyklad ddzialania procesu
      */
//    var content = "\"Oh, no,\" she's saying, \"our $400 blender can't handle something?this hard! Shops.+in new york are too fuck. London city has better shops.\""
//    val nerResult = NLPUtils.markNamedEntities(content)
//    val namedEntities = nerResult.subList(1, nerResult.size())
//    val tokens = NLPUtils.tokenize(nerResult.get(0))
//    var labels: util.List[CoreLabel] = NLPUtils.tagPartOfSpeech(tokens)
//    labels = NLPUtils.lemmatize(labels)
//    labels = NLPUtils.deleteStopWords(labels)
//    println("##labels##")
//    for (i <- 0 to labels.size()-1){
//      println(i+". "+labels.get(i).word()+" : "+labels.get(i).tag()+" : "+labels.get(i).lemma())
//    }
//    println("##named enitites##")
//    for (i <- 0 to namedEntities.size()-1){
//      println(i+". "+namedEntities.get(i))
//    }

    val dict = WordnetUtils.getDictionary()
    dict.open()

    val docs = new util.ArrayList[topicmodel.Document]() // lista wczytanych i przetworzonych dokumentow LDA

    var doc_1 = "big tits creampie porn pornhub baby girls big.cars.com cars hooker drugs lsd mdm shit.org"
    doc_1 = NLPUtils.removeUselessSuffixes(doc_1)       // usun przyrostki typu ".com", etc.
    doc_1 = NLPUtils.removeSomePunctuation(doc_1)       // usun czesc dziwnej interpunkcji
    val nerResult_1 = NLPUtils.markNamedEntities(doc_1)                 // NER
    val namedEntities_1 = nerResult_1.subList(1, nerResult_1.size())    // wez liste encji nazwanych
    val tokens_1 = NLPUtils.tokenize(nerResult_1.get(0))                // tokenizuj pozostale (nie bedace encjami) slowa
    var labels_1: util.List[CoreLabel] = NLPUtils.tagPartOfSpeech(tokens_1)   // POS tagging tokenow
    labels_1 = NLPUtils.lemmatize(labels_1)             // lematyzacja otagowanych tokenow
    labels_1 = NLPUtils.deleteStopWords(labels_1)       // usuniecie stopwords
    val words_1: util.List[String] = for (i <- labels_1.indices) yield labels_1.get(i).lemma()    // wyciagniecie samych lemm (stringow) z wyniku powyzszego przetwarzania
    val wordnetWords_1 = WordnetUtils.retrieveWordnetTerms(dict, words_1)             // wyodrebnij z danych lemm pojecia wordnetowe
    val otherWords_1: util.List[String] = for (w <- words_1 if !wordnetWords_1.contains(w)) yield w   // lista slow niebedacych encjami ani pojeciami wordnetowymi
    val document1 = new Document(1, namedEntities_1, wordnetWords_1, otherWords_1)      // stworz dokument LDA dla devID=1, z powyzszych zbiorow slow
    docs.add(document1)
    println(document1)

    var doc_2 = "bird species tits eating frogs ny central park cars pollution footprint save-planet.gov"
    doc_2 = NLPUtils.removeUselessSuffixes(doc_2)
    doc_2 = NLPUtils.removeSomePunctuation(doc_2)
    val nerResult_2 = NLPUtils.markNamedEntities(doc_2)
    val namedEntities_2 = nerResult_2.subList(1, nerResult_2.size())
    val tokens_2 = NLPUtils.tokenize(nerResult_2.get(0))
    var labels_2: util.List[CoreLabel] = NLPUtils.tagPartOfSpeech(tokens_2)
    labels_2 = NLPUtils.lemmatize(labels_2)
    labels_2 = NLPUtils.deleteStopWords(labels_2)
    val words_2: util.List[String] = for (i <- labels_2.indices) yield labels_2.get(i).lemma()
    val wordnetWords_2 = WordnetUtils.retrieveWordnetTerms(dict, words_2)
    val otherWords_2: util.List[String] = for (w <- words_2 if !wordnetWords_2.contains(w)) yield w
    val document2 = new Document(2, namedEntities_2, wordnetWords_2, otherWords_2)
    docs.add(document2)
    println(document2)

    var doc_3 = "aol google.com hotels big   parks+new york   estates london . cars warsaw city?restaurant;eat:frogs france"
    doc_3 = NLPUtils.removeUselessSuffixes(doc_3)
    doc_3 = NLPUtils.removeSomePunctuation(doc_3)
    val nerResult_3 = NLPUtils.markNamedEntities(doc_3)
    val namedEntities_3 = nerResult_3.subList(1, nerResult_3.size())
    val tokens_3 = NLPUtils.tokenize(nerResult_3.get(0))
    var labels_3: util.List[CoreLabel] = NLPUtils.tagPartOfSpeech(tokens_3)
    labels_3 = NLPUtils.lemmatize(labels_3)
    labels_3 = NLPUtils.deleteStopWords(labels_3)
    val words_3: util.List[String] = for (i <- labels_3.indices) yield labels_3.get(i).lemma()
    val wordnetWords_3 = WordnetUtils.retrieveWordnetTerms(dict, words_3)
    val otherWords_3: util.List[String] = for (w <- words_3 if !wordnetWords_3.contains(w)) yield w
    val document3 = new Document(3, namedEntities_3, wordnetWords_3, otherWords_3)
    docs.add(document3)
    println(document3)

    val numOfTopics = 4     // liczba tematow do zamodelowania
    val lda = new TopicModel(numOfTopics, 50, 0.1, 0.01)  // stworz niewytrenowany model LDA
    lda.train(docs, false)    // trenuj model LDA na zbiorze dokumentow 'docs' nie biorac pod uwage slow ze zbiorow 'pozostale'

    /**
      * pokaz dzialania MongoDB
      */
    val doc = DocumentDAO.getDocumentByDevice(document3.getDeviceID()) //WAZNE! zakladam w tej funkcji ze deviceId sa unikalne
    println(doc.toString())

    //pobierz wszystkie dokumenty, z projekcja
    val iterator = DocumentDAO.getAllDocuments(Array[String](DocumentDAO.Columns.NAMED_ENTITIES, DocumentDAO.Columns.DEVICE_ID))
    while (iterator.hasNext)
      println(iterator.next())

    //pobierz deviceQuery dla deviceId=1, z projekcja
    val it2 =DeviceQueryDAO.getDeviceQueriesByDevice(1, Array[String](DeviceQueryDAO.Columns.QUERY))
    while (it2.hasNext)
      println(it2.next())

    Database.client.close()
  }
}
