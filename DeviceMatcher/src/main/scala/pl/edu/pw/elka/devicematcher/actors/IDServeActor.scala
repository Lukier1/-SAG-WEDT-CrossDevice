package pl.edu.pw.elka.devicematcher.actors

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

/**
  * Created by lukier on 1/6/17.
  */
object IDServeActor {
    case class RangeID(begin : Int, end : Int)
}
class IDServeActor(workersNumber : Int) extends Actor {

  private var router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(Props[DBDevIDActor])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  private val maxDevicesForWorker = 3;

  import IDServeActor._

  private def splitID(begin : Int, end : Int) : Unit = {
      //var realEnd = end;
      if(end-begin < maxDevicesForWorker)
        {

           // NLPSuperVisior ! RangeID(begin, end)
          println(s"NLPSuperVisior ! RangeID(${begin}, ${end})")
           //
        }
      else {
        val maxEnd = end - (end - begin - maxDevicesForWorker)
        //NLPSuperVisior ! RangeID(begin, maxEnd)
        println(s"NLPSuperVisior ! RangeID(${begin}, ${maxEnd})")
        splitID(begin + maxDevicesForWorker, end)
      }
  }

  override def receive: Receive = {
    case RangeID(begin, end) => println("Looking for range: " + begin + " to " + end); router.route("job", sender())
    case _ => println("Unsupported message")
  }
}
