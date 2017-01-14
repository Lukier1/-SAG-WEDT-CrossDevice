package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.Actor
import akka.actor.Actor.Receive
import pl.edu.pw.elka.devicematcher.agents.actors.GroupsWorkerActor.ProcessForID
import pl.edu.pw.elka.devicematcher.utils.DevMatchLogger

/**
  * Created by lukeir on 14.01.17.
  */
object GroupsWorkerActor {
  case class ProcessForID(ID : Int)
}
class GroupsWorkerActor extends Actor{
  import GroupsWorkerActor._

  val LOGGER = DevMatchLogger.getLogger(getClass().getName())


  override def receive: Receive = {
    case ProcessForID(id : Int) => LOGGER.debug(s"Processign id : $id")
    case _ => LOGGER.warn("Unsupported message")
  }

}
