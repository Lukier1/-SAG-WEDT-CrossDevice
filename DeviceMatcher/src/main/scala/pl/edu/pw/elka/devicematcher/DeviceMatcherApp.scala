package pl.edu.pw.elka.devicematcher

import java.util

import org.apache.log4j
import pl.edu.pw.elka.devicematcher.data.{AnonDeviceDAO, Database, DocumentDAO}
import pl.edu.pw.elka.devicematcher.topicmodel.{Document, TopicModel, TopicModelSerializer}
import akka.actor.{ActorSystem, Props}
import pl.edu.pw.elka.devicematcher.agents.actors.IDServeActor
import akka.pattern.ask
import pl.edu.pw.elka.devicematcher.utils._

import scala.concurrent.duration._
import scala.concurrent.Await
import java.io._
import java.util


import scala.collection.JavaConversions._


object DeviceMatcherApp extends App {

  object utils {
    /**
      * Zakres przetwarzania:
      */
    val DO_STAGE_1 = false   // etap 1: przetwarzanie zapytan na dokumenty
    val DO_STAGE_2 = false   // etap 2: wczytanie dokumentow i trenowanie modelu LDA
    val DO_STAGE_3 = false  // etap 3: grupowanie dokumentow i zapis powiazan miedzy nimi
    val DO_STAGE_4 = false  // etap 4: ewaluacja wyniku grupowania dokumentow
    // parametry przetarzania etapu 1:
    val MIN_DEVID = 1
    val MAX_DEVID = 1000
    val NLP_WORKERS_COUNT = 4   // liczba workerów NLP

    /**
      * Parametry modelu LDA:
      */
    val TOPICS_COUNT = 200          // liczba modelowanych tematow wyszukiwan
    val ITERATIONS = 500            // iteracji algorytmu LDA
    val A = 0.05                    // alfa
    val B = 0.01                    // beta
    val SAVE_MODEL_TO_FILE = true   // flaga zapisu wytrenowanego modelu do pliku

    /**
      * Opcje dotyczace danych diagnostycznych modelu LDA zapisywanych do XMLow:
      */
    val DUMP_MODEL_DIAGNOSTICS = true   // zapisz diagnostyke
    val NUM_TOP_WORDS = 20              // top N slow per temat wyszczegolnionych w diagnostyce

    /**
      * Opcje dotyczace dodawania slow do dokumentow:
      */
    val WITH_OTHER_TERMS = false    // uwzgledniaj slowa ze zbioru 'pozostale'
    val ADD_HYPERONYMS = false      // dodaj do dokumentow rowniez hiperonimy slow ze zbioru 'pojecia'
    val ADD_SYNONYMS = false        // dodaj do dokumentow rowniez synonimy slow ze zbioru 'pojecia'

    val TO_FILE = true
    val TO_STDOUT = true
    val logger = DevMatchLogger.getLogger("DeviceMatcherApp", log4j.Level.DEBUG, TO_FILE, "app.log", TO_STDOUT)
  }
  import utils._

  override def main(args: Array[String]): Unit = {
    logger.info("System started...")
    logger.info("   do stage 1: " + DO_STAGE_1)
    logger.info("   do stage 2: " + DO_STAGE_2)
    logger.info("   do stage 3: " + DO_STAGE_3)
    logger.info("   do stage 4: " + DO_STAGE_4)

    if (DO_STAGE_1)
      processQueriesAndPrepareDocuments()

    var lda: TopicModel = null
    if (DO_STAGE_2)
      lda = readDocumentsAndFeedTopicModel()
    else
      lda = TopicModelSerializer.readTopicModelFromFile("model")
    if (lda == null) {
      logger.error("LDA model is null!")
      return
    }

    if (DUMP_MODEL_DIAGNOSTICS) {
      lda.writeDiagnosticsToXML("diagnostics")
      lda.writeTopicPhraseXMLReport("topics_phrases", NUM_TOP_WORDS)
      lda.writeTopicXMLReport("topics", NUM_TOP_WORDS)
    }

    if (SAVE_MODEL_TO_FILE && DO_STAGE_2) {
      logger.info("Saving trained topic model to file...")
      if (TopicModelSerializer.writeTopicModelToFile(lda, "model"))
        logger.info("   saved.")
      else
        logger.error("   could not serialize/save topic model to file.")
    }

    if (DO_STAGE_3)
      findConnectionsBetweenDocuments()

    if (DO_STAGE_4)
      evaluateFoundConnections()

    /**
      * przykład użycia metryk,
      * zakres od 9 do 15 z powodu brakujących urządzeń w bazie
      */
//    val docs = MatcherDataTest.metcicsDocs
//    FakeJSDivergence.isMetrics=true
//    var groups = MatcherUtils.getUntrimmedGroups(docs, 0.3, groupMembersSize = 3)
//
//    groups = MatcherUtils.trimGroups(0.3, groups, groupMembersSize = 3)
//    MatcherUtils.writeGroupsToFile(groups, "groups:" + 0.3 + "metrics.txt", trimmed = false)
//
//    val metrics = MetricsUtils.getBasicMetrics(groups,9,7)
//
//    logger.debug("Metrics: ")
//    logger.debug("True Positives: "+metrics(0))
//    logger.debug("False Positives: "+metrics(1))
//    logger.debug("True Negatives: "+metrics(2))
//    logger.debug("False Negatives: "+metrics(3))
//    logger.debug("Accuracy: "+MetricsUtils.accuracy(metrics(0),metrics(1),metrics(2),metrics(3)))
//    logger.debug("Precision: "+MetricsUtils.precision(metrics(0),metrics(1)))
//    logger.debug("Recall: "+MetricsUtils.recall(metrics(0),metrics(3)))

    Database.client.close()
  }

  /**
    * 1. etap przetwarzania: aktorzy odczytuja zapytania kolejnych urzadzen z bazy, przetwarzaja je
    * i tworza z nich dokumenty, ktore zostaja zapisane do bazy danych
    */
  def processQueriesAndPrepareDocuments(): Unit = {
    logger.info("Stage 1 starting...")
    logger.info(s"   processing devIDs in range: [$MIN_DEVID,$MAX_DEVID]")
    logger.info(s"   NLP_workers count: $NLP_WORKERS_COUNT")

    val actorsSystem = ActorSystem("System");
    val rootActor = actorsSystem.actorOf(Props(classOf[IDServeActor], NLP_WORKERS_COUNT))

    val processed = rootActor.ask(IDServeActor.RangeID(MIN_DEVID, MAX_DEVID))(8 hours)
    Await.result(processed, 8 hours).asInstanceOf[String]

    actorsSystem.terminate()

    logger.info("Stage 1 done.")
  }

  /**
    * 2. etap przetwarzania: odczyt wszystkich dokumentow z bazy danych i uzycie ich do wytrenowania
    * modelu LDA
    * @return wytrenowany model LDA
    */
  def readDocumentsAndFeedTopicModel(): TopicModel = {
    logger.info("Stage 2 starting...")
    logger.info(s"   {topics, iterations, alpha, beta} = {$TOPICS_COUNT, $ITERATIONS, $A, $B}")

    logger.info("Reading documents from database...")
    val docs = new util.ArrayList[Document]()
    val result = DocumentDAO.getAllDocuments()
    for (r <- result) {
      docs.add(DocumentDAO.fromDBObjectToDocument(r))
    }
    logger.info("   done.")
    Database.client.close()
    logger.debug("db closed")

    logger.info("Training topic model...")
    val lda = new TopicModel(TOPICS_COUNT, ITERATIONS, A, B)
    lda.train(docs, WITH_OTHER_TERMS)
    logger.info("   done.")

    logger.info("Stage 2 done.")
    lda
  }

  /**
    * 3. etap przetwarzania: analiza dokumentow urzadzen z wykorzystaniem wytrenowanego modelu LDA w poszukiwaniu
    * ich wzajemnych zaleznosci i zbudowania bazy powiazanych ze soba dokumentow
    */
  def findConnectionsBetweenDocuments(): Unit = {
    //TODO
  }

  /**
    * 4. etap: porownanie wyniku dzialania systemu z oczekiwaniami:
    * - obliczanie TN, TP, FN, FP porownujac wynik z oryginalnymi powiazaniami z bazy danych
    * - obliczenie metryk na podstawie powyzszych wartosci
    * - pokazanie wyniku uzytkownikowi
    */
  def evaluateFoundConnections(): Unit = {
    //TODO
  }

}
