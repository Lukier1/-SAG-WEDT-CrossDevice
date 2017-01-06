package pl.edu.pw.elka.devicematcher.actors

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

/**
  * Created by lukier on 1/6/17.
  */
class ExampleActor extends  Actor {
  val log = Logging(context.system, this)

  override def receive = {
    case "test" => log.info("received test")
    case _      => log.info("received unknown message")
  }
}
