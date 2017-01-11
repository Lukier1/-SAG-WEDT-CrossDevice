package pl.edu.pw.elka.devicematcher.agents.actors

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.log4j

import scala.concurrent.duration._
import pl.edu.pw.elka.devicematcher.agents.actors.NLPWorkerActor.DeviceIDProc
import pl.edu.pw.elka.devicematcher.utils.DevMatchLogger

import scala.concurrent.Await
/**
  * Wartswa proxy dla aktora prztwarzajacego NLP
  *
  * Created by lukier on 1/9/17.
  */
object  NLPProxyActor {
  val MAX_TRIES = 5

  val TO_FILE = true
  val TO_STDOUT = true
  val logger = DevMatchLogger.getLogger("NLPProxyActor", log4j.Level.DEBUG, TO_FILE, "nlpproxy.log", TO_STDOUT)
}

class NLPProxyActor(root : ActorRef) extends Actor {
  val endWork = context.actorOf(Props[NLPWorkerActor])
  val duration = 120 second
  var tries : Int = 0
  var id_act = 0

  import NLPProxyActor.logger

  //Odbior polecenia przetwarzania
  override def receive: Receive = {
    case NLPWorkerActor.DeviceIDProc(id : Int) =>
      logger.debug("'DeviceIDProc' received. ID: " + id)
      tries = 0
      id_act = id
      sendMsg
    case _ =>
      //println("Unrecongnized message")
      logger.error("Unrecongnized message received")
  }

  //Funkcja odpowiadaja za ponowne wysylanie wiadomosci gdy zostanie przerkoczony czas przetwarzania lub dostanie excpetiona
  private def sendMsg : Unit = {
    var success = false
    while(tries <= NLPProxyActor.MAX_TRIES && !success) {
      //println(s"Lets try $tries")
      logger.debug(s"(current devId: $id_act) tries: " + tries)
      tries += 1
      try {
        val future = endWork.ask(DeviceIDProc(id_act))(duration)
        Await.result(future, duration)
        match {
          case IDServeActor.Failed(_ : Int) => success = false
          case IDServeActor.Success(_ : Int) => success = true
        }
      }
      catch {
        case _ : TimeoutException =>
          logger.error(s"(current devId: $id_act) timeout exception")
          success = false
        case e : Exception =>
          logger.error(s"(current devId: $id_act) exception")
          success = false
      }

    }
    if(!success)
      {
        root ! IDServeActor.Failed(id_act)
        //println("Failed to : " + root)
        logger.error("failed to: " + root)
      }
    else
      {
        root ! IDServeActor.Success(id_act)
      }
  }
}
