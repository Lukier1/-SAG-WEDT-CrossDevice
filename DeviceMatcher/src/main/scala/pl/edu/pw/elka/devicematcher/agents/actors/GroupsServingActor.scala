package pl.edu.pw.elka.devicematcher.agents.actor

import java.util

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.BalancingPool
import pl.edu.pw.elka.devicematcher.agents.actors.{NLPProxyActor, NLPWorkerActor}
import pl.edu.pw.elka.devicematcher.topicmodel.Document
import pl.edu.pw.elka.devicematcher.utils.DevMatchLogger

/**
  * Created by lukeir on 14.01.17.
  */
object GroupsServingActor {
  /** case class Process
    *Polecenie przetwarzania, jezeli w argumencie podamy liczbe mniejsza od zera
    *zostaną przetworzone wszystkie dokumenty
    */
  case class Process(number : Int = -1)

  val THRESHOLD = 0.3f

  val WORKER_NUMBER = 10
}

class GroupsServingActor( _docs : util.List[Document]) extends Actor  {
  import GroupsServingActor._

  val workerRouter : ActorRef = context.actorOf(BalancingPool(WORKER_NUMBER).props(Props(classOf[Group], self)))

  val LOGGER  = DevMatchLogger.getLogger(this.getClass().getName, this.getClass().getName() + ".log")
  val docs: util.List[Document] = _docs

  override def receive: Receive = {
    case Process(number : Int) => processDevices(number)
    case _ => LOGGER.warn("Nieznana wiadomosc")
  }

  private def processDevices(number : Int): Unit = {
    val calcEnd = () => {
      if(number > docs.size() || number < 0) {
        docs.size()
      }
      number
    }
    val end : Int = calcEnd() //TODO: Wie ktoś jak to skrócić? Tak żeby za = była funkcja anonimowa

    for (idDocs <-1 until end) {
      LOGGER.debug(s"Processing $idDocs")
    }
  }
}
