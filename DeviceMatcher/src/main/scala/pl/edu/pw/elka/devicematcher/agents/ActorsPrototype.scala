package pl.edu.pw.elka.devicematcher.agents

import akka.actor.{ActorSystem, Props}
import pl.edu.pw.elka.devicematcher.agents.actors.IDServeActor
import akka.pattern.ask
import pl.edu.pw.elka.devicematcher.agents.actor.GroupsServingActor
import pl.edu.pw.elka.devicematcher.agents.actor.GroupsServingActor.Process
import java.util

import pl.edu.pw.elka.devicematcher.topicmodel.Document
import pl.edu.pw.elka.devicematcher.utils.{MatcherDataTest, MatcherUtils}

import scala.concurrent.duration._
import scala.concurrent.Await
/**
  * Created by lukier on 1/6/17.
  */
object ActorsPrototype  extends App {
  override def main(args: Array[String]): Unit = {
    main()
  }
  def main(): Unit = {
    println("Actors prototype start")

    val actorsSystem = ActorSystem("System");
    //val rootActor = actorsSystem.actorOf(Props(classOf[IDServeActor],5))
    val testList : util.List[Document] = MatcherDataTest.docs

    val result = MatcherUtils.getUntrimmedGroups(testList, 0.3);
    println("Out of function : \n" + result)


    val rootActor = actorsSystem.actorOf(Props(classOf[GroupsServingActor],testList))
    //val processed = rootActor.ask(IDServeActor.RangeID(1, 5))(8 hours)
    val processed = rootActor.ask(Process())(8 hours);
    Await.result(processed, 8 hours) match {
      case GroupsServingActor.Result(list) => println("Out of agents: \n" + list)
    }

    actorsSystem.terminate()
  }
}
