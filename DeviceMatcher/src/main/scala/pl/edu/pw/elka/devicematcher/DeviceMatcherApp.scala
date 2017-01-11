package pl.edu.pw.elka.devicematcher

import java.util
import org.apache.log4j
import pl.edu.pw.elka.devicematcher.data.{Database, DocumentDAO}
import pl.edu.pw.elka.devicematcher.topicmodel.{Document, TopicModel, TopicModelSerializer}
import akka.actor.{ActorSystem, Props}
import pl.edu.pw.elka.devicematcher.agents.actors.IDServeActor
import akka.pattern.ask
import pl.edu.pw.elka.devicematcher.utils.DevMatchLogger
import scala.concurrent.duration._
import scala.concurrent.Await


object DeviceMatcherApp extends App {

  object utils {
    val MIN_DEVID = 1
    val MAX_DEVID = 20
    val NLP_WORKERS_COUNT = 3

    val TOPICS_COUNT = 200
    val ITERATIONS = 500
    val A = 0.05
    val B = 0.01

    val WITH_OTHER_TERMS = false
    val ADD_HYPERONYMS = false
    val ADD_SYNONYMS = false

    val TO_FILE = true
    val TO_STDOUT = true
    val logger = DevMatchLogger.getLogger("DeviceMatcherApp", log4j.Level.DEBUG, TO_FILE, "app.log", TO_STDOUT)
  }
  import utils._

  override def main(args: Array[String]): Unit = {
    logger.info("System started...")
    processQueriesAndPrepareDocuments()
    //val lda = readDocumentsAndFeedTopicModel(TOPICS_COUNT, ITERATIONS, A, B, WITH_OTHER_TERMS)
    //findConnectionsBetweenDocuments()
    //evaluateFoundConnections()
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

    logger.info("Saving trained topic model to file...")
    if (TopicModelSerializer.writeTopicModelToFile(lda, "model_" + TOPICS_COUNT + "topics"))
      logger.info("   saved.")
    else
      logger.error("   could not serialize/save topic model to file.")

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
