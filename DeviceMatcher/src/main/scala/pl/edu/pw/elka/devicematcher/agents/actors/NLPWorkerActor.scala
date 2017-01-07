package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
  * Created by lukier on 1/7/17.
  */
object NLPWorkerActor {
  case class DeviceIDProc(deviceId : Int)
}
class NLPWorkerActor extends Actor {
  import NLPWorkerActor._
  override def receive: Receive = {
    case DeviceIDProc(devId) => println(s"Process device number: $devId")
    case _ => println("NLPWorker - Undefined msg.")
  }
}
