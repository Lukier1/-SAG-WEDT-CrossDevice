package pl.edu.pw.elka.devicematcher

import java.io._
import java.util

import edu.mit.jwi.IDictionary
import edu.stanford.nlp.ling.CoreLabel
import pl.edu.pw.elka.devicematcher.data.{Database, DeviceQueryDAO}
import pl.edu.pw.elka.devicematcher.topicmodel.{Document, TopicModel}
import pl.edu.pw.elka.devicematcher.utils.{NLPUtils, WordnetUtils}

import scala.collection.JavaConversions._


object DeviceMatcherApp extends App {

  /**
    * Przygotowanie dokumentu LDA danego urządzenia na podstawie listy zapytań.
    *
    * @param devId id urządzenia
    * @param queries lista zapytan urzadzenia
    * @param dict otwarty slownik Wordnet potrzebny do przetwarzania słów zapytań
    * @return utworzony dokument LDa reprezentujący zapytania urządzenia o zadanym id
    */
  def prepareDocument(devId: Int, queries: util.List[String], dict: IDictionary): Document = {
    val namedEntities = new util.ArrayList[String]()
    val wordnetWords = new util.ArrayList[String]()
    val otherWords = new util.ArrayList[String]()

    for (q <- queries) {
      var doc = NLPUtils.removeUselessSuffixes(q)
      doc = NLPUtils.removeSomePunctuation(doc)

      val nerResult = NLPUtils.markNamedEntities(doc)
      namedEntities.addAll(nerResult.subList(1, nerResult.size()))

      val tokens = NLPUtils.tokenize(nerResult.get(0))
      var labels: util.List[CoreLabel] = NLPUtils.tagPartOfSpeech(tokens)
      labels = NLPUtils.lemmatize(labels)
      labels = NLPUtils.deleteStopWords(labels)

      val words: util.List[String] = for (i <- labels.indices) yield labels.get(i).lemma()
      val fromWordnet = WordnetUtils.retrieveWordnetTerms(dict, words)
      wordnetWords.addAll(fromWordnet)
      val others: util.List[String] = for (w <- words if !fromWordnet.contains(w)) yield w
      otherWords.addAll(others)
    }

    return new Document(devId, namedEntities, wordnetWords, otherWords)
  }

  /**
    * Serializacja i zapis danego modelu LDA do pliku.
    *
    * @param model model LDA do zapisu
    * @param filename nazwa pliku
    * @return true jeśli zapis powiódł się, false w p.p.
    */
  def writeTopicModelToFile(model: TopicModel, filename: String): Boolean = {
    try {
      val oos =  new ObjectOutputStream(new FileOutputStream("./src/main/resources/models/" + filename))
      oos.writeObject(model)
      oos.close()
      return true
    } catch {
      case e: Exception =>
        return false
    }
  }

  /**
    * Odczyt i deserializacja modelu LDA z pliku.
    *
    * @param filename nazwa pliku
    * @return model LDA lub null, gdy się nie powiedzie odczyt/deserializacja
    */
  def readTopicModelFromFile(filename: String): TopicModel = {
    try {
      val ois = new ObjectInputStream(new FileInputStream("./src/main/resources/models/" + filename))
      val model = ois.readObject.asInstanceOf[TopicModel]
      ois.close()
      return model
    } catch {
      case e: Exception =>
        return null
    }
  }

  override def main(args: Array[String]): Unit = {
    val MIN_DEVID = 1
    val MAX_DEVID = 1000

    val dict = WordnetUtils.getDictionary()
    dict.open()

    val docs = new util.ArrayList[Document]()

    for (i <- MIN_DEVID to MAX_DEVID) {
      val iterator = DeviceQueryDAO.getDeviceQueriesByDevice(i)
      if (iterator.nonEmpty) {
        val queries = new util.ArrayList[String]()
        for (it <- iterator) queries.add(it.get("Query").asInstanceOf[String])
        val document = prepareDocument(i, queries, dict)
        docs.add(document)
      }
    }

    Database.client.close()

    val iterations = List[Int](50, 500, 1000, 1500, 2000)
    //val alfa_beta = List[(Double,Double)]((0.01,0.1), (0.05,0.1), (0.1,0.1), (0.2,0.1), (0.05,0.002), (0.05,0.01), (0.05,1.0))

    //for (ab <- alfa_beta) {
    for (it <- iterations) {
      val TOPICS_COUNT = 200
      //val ITERATIONS = 500
      val ITERATIONS = it
      //val A = ab._1
      //val B = ab._2
      val A = 0.05
      val B = 0.01
      val WITH_OTHER_TERMS = false
      val ADD_HYPERONYMS = false
      val ADD_SYNONYMS = false

      val name = "topics_" + TOPICS_COUNT + "_its_" + ITERATIONS + "_alfa_" + A + "_beta_" + B
      val dir = new File("./src/main/resources/reports/" + name)
      dir.mkdir()
      val file = new File("./src/main/resources/reports/" + name + "/" + name + ".txt")
      val logger = new PrintWriter(file)

      logger.println("\nTrening modelu:")
      logger.println("-- liczba tematow: " + TOPICS_COUNT)
      logger.println("-- liczba iteracji: " + ITERATIONS)
      logger.println("-- alfa: " + A)
      logger.println("-- beta: " + B)
      logger.println("-- modeluj dla 'pozostale': " + WITH_OTHER_TERMS)
      logger.println("-- dodaj hiperonimy: " + ADD_HYPERONYMS)
      logger.println("-- dodaj synonimy: " + ADD_SYNONYMS)

      val lda = new TopicModel(TOPICS_COUNT, ITERATIONS, A, B)
      lda.train(docs, WITH_OTHER_TERMS)

      //    logger.println("\nRozkłady tematów dla dokumentów:")
      for (d <- docs) {
        val probs = lda.getTopicDistributionByDevID(d.getDeviceID())
        d.setTopicDistribution(probs)
        //      logger.print("Document nr " + d.getDeviceID() + ": ")
        //      for (j <- 0 until TOPICS_COUNT) {
        //        logger.print(probs(j) + ", ")
        //      }
        //      logger.println()
      }

      logger.println("Temat (top 15 słów):")
      val dataAlphabet = lda.getAlphabet()
      val topicSortedWords = lda.getTopicSortedWords()
      for (i <- 0 until TOPICS_COUNT) {
        logger.print("Temat nr " + i + ": ")
        val iterator = topicSortedWords.get(i).iterator()
        var rank = 0
        while (iterator.hasNext && rank < 15) {
          val idCountPair = iterator.next()
          logger.print(dataAlphabet.lookupObject(idCountPair.getID()) + ",")
          rank += 1
        }
        logger.println()
      }

      logger.close()

      lda.writeDiagnosticsToXML(name + "/" + name + "_diagnostics")
      lda.writeTopicXMLReport(name + "/" + name + "_topics_top15", 15)
      lda.writeTopicPhraseXMLReport(name + "/" + name + "topics_phrases_top15", 15)
    }

  }
}
