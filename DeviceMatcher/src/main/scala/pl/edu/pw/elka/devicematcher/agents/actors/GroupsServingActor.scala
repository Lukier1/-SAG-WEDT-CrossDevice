package pl.edu.pw.elka.devicematcher.agents.actor

import java.util

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.BalancingPool
import pl.edu.pw.elka.devicematcher.agents.actors.IDServeActor.Success
import pl.edu.pw.elka.devicematcher.agents.actors.GroupsWorkerActor
import pl.edu.pw.elka.devicematcher.topicmodel.Document
import pl.edu.pw.elka.devicematcher.utils.{DevMatchLogger, Group, MatcherUtils}

/**
  * Created by lukeir on 14.01.17.
  */
object GroupsServingActor {
  /** case class Process
    *Polecenie przetwarzania, jezeli w argumencie podamy liczbe mniejsza od zera
    *zostaną przetworzone wszystkie dokumenty
    */
  case class Process(number : Int = -1)
  case class Result(result : util.List[Group])
  val THRESHOLD = 0.3f

  val WORKER_NUMBER = 10

  val SIZE_OF_BUCKET = 4
}

class GroupsServingActor( _docs : util.List[Document]) extends Actor  {
  import GroupsServingActor._
  import GroupsWorkerActor._
  val workerRouter : ActorRef = context.actorOf(BalancingPool(WORKER_NUMBER).props(Props(classOf[GroupsWorkerActor])))

  val LOGGER  = DevMatchLogger.getLogger(this.getClass().getName(), this.getClass().getName() + ".log")
  val docs: util.List[Document] = _docs

  var runnerRef : ActorRef = self

  var successedGroups = 0
  var numberToProcess = 0;

  var totalList : util.List[Group] = new util.ArrayList[Group]()

  override def receive: Receive = {
    case Process(number : Int) => runnerRef = sender();   processDevices(number)
    case GroupsWorkerActor.Success(list : util.List[Group]) =>
      successedGroups+=1;
      LOGGER.debug(s"Succesfully processed this numbers of groups: $successedGroups");
      totalList.addAll(list);
      if(successedGroups > numberToProcess) {
        val resultList  = MatcherUtils.trimGroups(THRESHOLD, totalList)
        runnerRef ! Result(resultList)
      }
    case Failed(id : Int) =>
      LOGGER.error(s"Cant' process: $id");
      workerRouter ! ProcessForID(id, docs)
    case _ => LOGGER.warn("Nieznana wiadomosc")
  }

  private def processDevices(number : Int): Unit = {

    val calcEnd = () => {
      if(number > docs.size() || number < 0) {
        docs.size()
      }
      else {
        number
      }
    }
    val end : Int = calcEnd() //TODO: Wie ktoś jak to skrócić? Tak żeby za = była funkcja anonimowa

    val rounds = Math.floor((end+1)/SIZE_OF_BUCKET);

    numberToProcess = rounds.toInt;


    for (idDocs <-0 until rounds.toInt+1) {

      LOGGER.debug(s"Processing $idDocs");
      workerRouter ! ProcessForID(idDocs*SIZE_OF_BUCKET, docs)

    }

  }
}
