package pl.edu.pw.elka.devicematcher.agents.actors

import java.util

import akka.actor.Actor
import akka.actor.Actor.Receive
import org.apache.log4j
import pl.edu.pw.elka.devicematcher.data.{DeviceQueryDAO, DocumentDAO}
import pl.edu.pw.elka.devicematcher.topicmodel.Document
import pl.edu.pw.elka.devicematcher.utils.{DevMatchLogger, WordnetUtils}

/**
  * Aktor przetwarzajacy NLP dane Device ID
  *
  * Created by lukier on 1/7/17.
  */
object NLPWorkerActor {
  case class DeviceIDProc(deviceId : Int)
}
class NLPWorkerActor extends Actor {
  import NLPWorkerActor._

  private val dict = WordnetUtils.getDictionary()
  dict.open()

  val TO_FILE = true
  val TO_STDOUT = true
  val logger = DevMatchLogger.getLogger("NLPWorkerActor", log4j.Level.DEBUG, TO_FILE, "nlpworkers.log", TO_STDOUT)

  //Funkcja przetwarzajaca
  def processing(id : Int) : Unit = {
    logger.debug(s"Processing devId = $id...")
    val iterator = DeviceQueryDAO.getDeviceQueriesByDevice(id)
    if (iterator.nonEmpty) {
      val queries = new util.ArrayList[String]()
      for (it <- iterator) queries.add(it.get("Query").asInstanceOf[String])
      val document = Document.prepareDocument(id, queries, dict)
      logger.debug(s"   id:$id - doc: " + document)
      //DocumentDAO.addDocument(document)
    }
    logger.debug(s"   processing devId = $id done.")
  }

  //Odbieramy polecenie przetwarzania
  override def receive: Receive = {
    case DeviceIDProc(devId) =>
      try {
        processing(devId)
        sender() ! IDServeActor.Success(devId)
      }
      catch {
        case e : Exception =>
          //println(s"Error msg for $devId is $e" )
          //logger.error(s"Error message for $devId is: $e") // To powoduje blad
          sender() !  IDServeActor.Failed(devId)
      }
    case _ =>
      //println("NLPWorker - Undefined msg.")
     // logger.error("Undefined message received.")
  }

}
