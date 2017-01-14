package pl.edu.pw.elka.devicematcher.agents.actors

import java.util

import akka.actor.Actor
import pl.edu.pw.elka.devicematcher.agents.actor.GroupsServingActor
import pl.edu.pw.elka.devicematcher.topicmodel.Document
import pl.edu.pw.elka.devicematcher.utils.{DevMatchLogger, Group, MatcherUtils, MetricsUtils}

/**
  * Created by lukeir on 14.01.17.
  */
object GroupsWorkerActor {
  case class ProcessForID(ID : Int, list : util.List[Document])
  case class Success(list : util.List[Group])
  case class Failed(ID : Int)
}
class GroupsWorkerActor extends Actor{
  import GroupsWorkerActor._

  val LOGGER = DevMatchLogger.getLogger(getClass().getName())

  override def receive: Receive = {

    case ProcessForID(id: Int, list: util.List[Document]) => {
      try {

        var range = GroupsServingActor.SIZE_OF_BUCKET;
        val diff = list.size()-id;
        if(diff < GroupsServingActor.SIZE_OF_BUCKET)
          {
            range = diff;
          }
        val outGroups = MatcherUtils.getUntrimmedGroups(list, GroupsServingActor.THRESHOLD, id, range = range)
        LOGGER.debug(s"Process device id $id for range $range");
        LOGGER.debug(s"Group for this $outGroups");
        sender() ! Success(outGroups)
      }
      catch {

        case e : Exception => LOGGER.error(e); sender() ! Failed(id)
      }

    }
    case _ => LOGGER.warn("Unsupported message")
  }

}
