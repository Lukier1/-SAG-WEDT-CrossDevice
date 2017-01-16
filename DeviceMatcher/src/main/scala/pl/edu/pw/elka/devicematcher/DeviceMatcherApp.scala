package pl.edu.pw.elka.devicematcher

import org.apache.log4j
import pl.edu.pw.elka.devicematcher.data.{Database, DocumentDAO}
import pl.edu.pw.elka.devicematcher.topicmodel.{Document, TopicModel, TopicModelSerializer}
import akka.actor.{ActorSystem, Props}
import pl.edu.pw.elka.devicematcher.agents.actors.IDServeActor
import akka.pattern.ask
import pl.edu.pw.elka.devicematcher.utils._

import scala.concurrent.duration._
import scala.concurrent.Await
import java.util

import pl.edu.pw.elka.devicematcher.agents.actor.GroupsServingActor
import pl.edu.pw.elka.devicematcher.agents.actor.GroupsServingActor.Process

import scala.collection.JavaConversions._


object DeviceMatcherApp extends App {

  object utils {
    /**
      * Zakres przetwarzania:
      */
    val DO_STAGE_1 = true    // etap 1: przetwarzanie zapytan na dokumenty
    val DO_STAGE_2 = true    // etap 2: wczytanie dokumentow i trenowanie modelu LDA
    val DO_STAGE_3 = true    // etap 3: grupowanie dokumentow i zapis powiazan miedzy nimi
    val DO_STAGE_4 = true    // etap 4: ewaluacja wyniku grupowania dokumentow
    // zakres przetwarzania
    val MIN_DEVID = 1
    val MAX_DEVID = 100
    // parametry etapu 1:
    val NLP_WORKERS_COUNT = 4         // liczba workerów NLP
    // parametry etapu 2:
    val TOPIC_MODELING_THREADS = 4    // liczba watkow uczenia modelu LDA
    // parametry etapu 3:
    val GROUPING_WORKERS_COUNT = 4    // liczba workerów grupujących dokumenty urzadzen
    val DIV_THRESHOLD = 0.2f          // prog dywergencji J-S dla ktorej akceptowane jest powiazanie urzadzen
    val BUCKET_SIZE = 4               // pojemnosc kubelkow w 1. fazie etapu 3 (= zakladana maks. liczba urzadzen per uzytkownik)
    // parametry etapu 4:
    val WRITE_GROUPS_TO_FILE = true

    /**
      * Parametry modelu LDA:
      */
    val TOPICS_COUNT = 50          // liczba modelowanych tematow wyszukiwan
    val ITERATIONS = 50            // iteracji algorytmu LDA
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
//    val ADD_HYPERONYMS = false      // dodaj do dokumentow rowniez hiperonimy slow ze zbioru 'pojecia'
//    val ADD_SYNONYMS = false        // dodaj do dokumentow rowniez synonimy slow ze zbioru 'pojecia'

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

    logger.info("Dropping Document collection...")
    DocumentDAO.clearCollection()
    logger.info("   done.")

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

    var groupedDevIds: util.List[Group] = null
    if (DO_STAGE_3)
      groupedDevIds = findConnectionsBetweenDocuments()

    if (DO_STAGE_4 && groupedDevIds != null)
      evaluateFoundConnections(groupedDevIds)

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
    logger.info(s"   {topics, threads, iterations, alpha, beta} = {$TOPICS_COUNT, $TOPIC_MODELING_THREADS, $ITERATIONS, $A, $B}")

    logger.info("Reading documents from database...")
    val docs = new util.ArrayList[Document]()
    val result = DocumentDAO.getAllDocuments()
    for (r <- result) {
      docs.add(DocumentDAO.fromDBObjectToDocument(r))
    }

    logger.info("Training topic model...")
    val lda = new TopicModel(TOPICS_COUNT, ITERATIONS, A, B)
    lda.train(docs, TOPIC_MODELING_THREADS, WITH_OTHER_TERMS)
    logger.info("   done.")

    logger.info("Saving topic distributions in documents into database...")
    for (d <- docs) {
      DocumentDAO.updateDocument(d.getDeviceID(), lda.getTopicDistributionByDevID(d.getDeviceID()))
    }
    logger.info("   done.")

    logger.info("Stage 2 done.")
    lda
  }

  /**
    * 3. etap przetwarzania: analiza dokumentow urzadzen z wykorzystaniem wytrenowanego modelu LDA w poszukiwaniu
    * ich wzajemnych zaleznosci i zbudowania bazy powiazanych ze soba dokumentow
    *
    * @return liste grup powiazanych devId
    */
  def findConnectionsBetweenDocuments(): util.List[Group] = {
    logger.info("Stage 3 starting...")
    logger.info(s"   processing devIDs in range: [$MIN_DEVID,$MAX_DEVID]")
    logger.info(s"   GROUPING_workers count: $GROUPING_WORKERS_COUNT")
    logger.info(s"J-S divergence threshold: $DIV_THRESHOLD")

    val result = DocumentDAO.getDocumentsFromRange(MIN_DEVID, MAX_DEVID)
    val docs = new util.ArrayList[Document]()
    for (r <- result) {
      docs.add(DocumentDAO.fromDBObjectToDocument(r))
    }
    var groupedDevIds : util.List[Group] = null

    val actorsSystem = ActorSystem("System")
    val rootActor = actorsSystem.actorOf(Props(classOf[GroupsServingActor], GROUPING_WORKERS_COUNT, DIV_THRESHOLD, BUCKET_SIZE, docs))

    val processed = rootActor.ask(Process())(8 hours)
    Await.result(processed, 8 hours) match {
      case GroupsServingActor.Result(list) =>
        groupedDevIds = list
    }
    actorsSystem.terminate()

    logger.info("Stage 3 done.")

    groupedDevIds
  }

  /**
    * 4. etap: porownanie wyniku dzialania systemu z oczekiwaniami:
    * - obliczanie TN, TP, FN, FP porownujac wynik z oryginalnymi powiazaniami z bazy danych
    * - obliczenie metryk na podstawie powyzszych wartosci
    * - pokazanie wyniku uzytkownikowi
    *
    * @param groups lista grup powiazanych devId
    */
  def evaluateFoundConnections(groups: util.List[Group]): Unit = {
    logger.info("Stage 4 starting...")
    logger.info("   calculating metrics...")

    if (WRITE_GROUPS_TO_FILE) {
      if (!MatcherUtils.writeGroupsToFile(groups, "groups_th_" + DIV_THRESHOLD + "_metrics.txt", trimmed = true))
        logger.error("Saving list of grouped devIds to file failed.")
    }

    val metrics = MetricsUtils.getBasicMetrics(groups, startIndex = MIN_DEVID, range = MAX_DEVID+1)
    val tp = metrics(0)
    val fp = metrics(1)
    val tn = metrics(2)
    val fn = metrics(3)
    val precision = MetricsUtils.precision(tp, fp)
    val recall = MetricsUtils.recall(tp, fn)

    logger.info("Metrics: ")
    logger.debug("True Positives: " + tp)
    logger.debug("False Positives: "+ + fp)
    logger.debug("True Negatives: " + tn)
    logger.debug("False Negatives: " + fn)
    logger.info("Precision: " + precision)
    logger.info("Recall: " + recall)
    logger.info("Accuracy: " + MetricsUtils.accuracy(tp, fp, tn, fn))
    logger.info("Specificity: " + MetricsUtils.specificity(tn, fp))
    logger.info("F-measure: " + MetricsUtils.f_measure(precision, recall))

    logger.info("Stage 4 done.")
  }

}
