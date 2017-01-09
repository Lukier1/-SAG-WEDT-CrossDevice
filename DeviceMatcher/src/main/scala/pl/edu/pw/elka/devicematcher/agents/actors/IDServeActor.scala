package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{BalancingPool, RoundRobinPool}
import pl.edu.pw.elka.devicematcher.agents.actors.NLPWorkerActor.DeviceIDProc

/**
  * Aktor do dzielenia  id urzadzen pomiedzy aktorów którzy będa je pobierali z bazy danych
  *
  * Created by lukier on 1/6/17.
  */
object IDServeActor {
    case class RangeID(begin : Int, end : Int)
    val WORKER_NUMBER = 10
    case class Success(id : Int)
    case class Failed(id : Int)
}
class IDServeActor(workersNumber : Int) extends Actor {
  import IDServeActor._

  //Router do rozsyłania wiadomości,
  val workerRouter : ActorRef = context.actorOf(BalancingPool(WORKER_NUMBER).props(Props(classOf[NLPProxyActor], self)))
  var deviceNumber  : Int =  0
  var processedDevices : Int = 0

  var systemSender : ActorRef = self

  //Odbior wiadomosci od roota i od workerow
  override def receive: Receive = {
    case RangeID(begin, end) =>
      systemSender = sender()
      deviceNumber = end-begin
      splitID(begin,end+1)
    case Success(_ : Int) =>
      deviceNumber += 1
      if(processedDevices >= 0.999 * deviceNumber) // Gdy zostanie przetworzone 0.999 wysyła wiadomośc potwierdzającą
        systemSender ! "done"
    case Failed(id : Int) =>  workerRouter ! DeviceIDProc(id)
    case _ => println("Unsupported message")
  }

  private def splitID(begin : Int, end : Int) : Unit = {
    for (id <- begin until end) {
      workerRouter ! DeviceIDProc(id)
    }
  }


}
