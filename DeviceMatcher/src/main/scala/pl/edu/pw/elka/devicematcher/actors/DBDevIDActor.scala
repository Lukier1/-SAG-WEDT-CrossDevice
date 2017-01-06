package pl.edu.pw.elka.devicematcher.actors

import akka.actor.Actor

/**
  * Created by lukier on 1/6/17.
  */
object DBDevIDActor {}

class DBDevIDActor extends Actor{
  import DBDevIDActor._

  override def receive: Receive = {
    case "job" => println(s"Do job: $self"); Thread.sleep(500)
    case _ => println(s"Not supported message for $self")
  }
}
