package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.Actor
import akka.actor.Actor.Receive

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

  //Funkcja przetwarzajaca
  def processing(id : Int) : Unit = {
      println(s"NLP Processing $id")
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
          println(s"Error msg for $devId is $e" )
          sender() !  IDServeActor.Failed(devId)
      }
    case _ => println("NLPWorker - Undefined msg.")
  }
}
