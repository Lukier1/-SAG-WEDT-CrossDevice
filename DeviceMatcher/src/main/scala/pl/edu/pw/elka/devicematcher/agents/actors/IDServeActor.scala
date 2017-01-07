package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{ BalancingPool }


/**
  * Created by lukier on 1/6/17.
  */
object IDServeActor {
    case class RangeID(begin : Int, end : Int)
    val DB_ACTOR_NUM = 4
    val MAX_DEVICES_ID_FOR_ACTOR = 3
}
class IDServeActor(workersNumber : Int) extends Actor {
  import IDServeActor._

  val DBDeviceRouter : ActorRef = context.actorOf(BalancingPool(DB_ACTOR_NUM).props(Props[DBDevIDActor]), "DBDeviceRouter")

  override def receive: Receive = {
    case RangeID(begin, end) => splitID(begin,end)
    case _ => println("Unsupported message")
  }

  private def splitID(begin : Int, end : Int) : Unit = {
      if(end-begin < MAX_DEVICES_ID_FOR_ACTOR)
        {
          DBDeviceRouter ! RangeID(begin, end)
          println(s"NLPSuperVisior ! RangeID(${begin}, ${end})")

        }
      else {
        val maxEnd = end - (end - begin - MAX_DEVICES_ID_FOR_ACTOR)
        DBDeviceRouter ! RangeID(begin, maxEnd)
        println(s"NLPSuperVisior ! RangeID(${begin}, ${maxEnd})")
        splitID(begin + MAX_DEVICES_ID_FOR_ACTOR, end)
      }
  }


}
