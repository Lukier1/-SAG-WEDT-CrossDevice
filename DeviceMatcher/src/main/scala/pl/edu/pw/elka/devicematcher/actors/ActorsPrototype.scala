package pl.edu.pw.elka.devicematcher.actors

import akka.actor.{ActorSystem, Props}

/**
  * Created by lukier on 1/6/17.
  */
object ActorsPrototype {
  def main(): Unit = {
    println("Actors prototype start")

    val actorsSystem = ActorSystem("System")
    val actor = actorsSystem.actorOf(Props[ExampleActor])
    println(actor)

    val rootActor = actorsSystem.actorOf(Props(classOf[IDServeActor],5))
    val future = actor ! "test"
    actor ! ExampleActor.Greeting("DUPA")
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    rootActor ! IDServeActor.RangeID(1, 5)
    
    Thread.sleep(1000)

    println(future )

    actorsSystem.terminate()

    println("Actors prototype finish")
  }
}
