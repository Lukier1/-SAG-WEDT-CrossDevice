package pl.edu.pw.elka.devicematcher.actors

import akka.actor.{Actor, Props}
import akka.event.Logging

/**
  * Created by lukier on 1/6/17.
  */
object ExampleActor {
  case class Greeting(from : String)
}

class ExampleActor extends Actor
{
  import ExampleActor._

  val log = Logging(context.system, this)

  override def receive = {
    case Greeting(greeter) => log.info(s"Greeting by $greeter .")
    case "test" => log.info("received test")
    case _      => log.info("received unknown message")
  }
}
