package pl.edu.pw.elka.devicematcher.agents

import akka.actor.{ActorSystem, Props}
import pl.edu.pw.elka.devicematcher.agents.actors.{IDServeActor}

/**
  * Created by lukier on 1/6/17.
  */
object ActorsPrototype  extends App {
  override def main(args: Array[String]): Unit = {
    main()
  }
  def main(): Unit = {

    println("Actors prototype start")

    val actorsSystem = ActorSystem("System")

    val rootActor = actorsSystem.actorOf(Props(classOf[IDServeActor],5))
    rootActor ! IDServeActor.RangeID(1, 100)

    Thread.sleep(4000)

    actorsSystem.terminate()

    println("Actors prototype finish")
  }
}
