package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.BalancingPool
import pl.edu.pw.elka.devicematcher.agents.actors.NLPWorkerActor.DeviceIDProc

/**
  * Created by lukier on 1/6/17.
  */
object DBDevIDActor {
  val r = scala.util.Random
  val WORKER_NUMER = 2
}

class DBDevIDActor extends Actor{
  import DBDevIDActor._
  import IDServeActor.RangeID

  override def receive: Receive = {
    case RangeID(begin, end) => splitJob(begin, end)
    case _ => println(s"Not supported message for $self")
  }

  val workerRouter : ActorRef = context.actorOf(BalancingPool(WORKER_NUMER).props(Props[NLPWorkerActor]), "workersRouter")

  private def splitJob(begin : Int, end : Int) = {
    println(s"Do job in range $begin - $end");
    var id = 0
    for(id <- begin until end)
      {
        workerRouter ! DeviceIDProc(id)
      }
  }
}
