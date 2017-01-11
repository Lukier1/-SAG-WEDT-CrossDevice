package pl.edu.pw.elka.devicematcher.agents.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{BalancingPool, RoundRobinPool}
import edu.stanford.nlp.parser.lexparser.BiLexPCFGParser.N5BiLexPCFGParser
import org.apache.log4j
import pl.edu.pw.elka.devicematcher.agents.actors.NLPWorkerActor.DeviceIDProc
import pl.edu.pw.elka.devicematcher.utils.DevMatchLogger

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

    val TO_FILE = true
    val TO_STDOUT = true
    val logger = DevMatchLogger.getLogger("IDServeActor", log4j.Level.DEBUG, TO_FILE, "idserveactor.log", TO_STDOUT)
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
      logger.debug("'RangeID' message received.")
      systemSender = sender()
      deviceNumber = end-begin+1
      logger.debug(s"   splitting IDs: [begin,end] = [$begin,$end]")
      splitID(begin,end+1)
      logger.debug("   splitting IDs done.")
    case Success(_ : Int) =>
      processedDevices += 1
      logger.info("Doc processed. Remaining: " + (deviceNumber-processedDevices))
      if(processedDevices >= 0.999 * deviceNumber) // Gdy zostanie przetworzone 0.999 wysyła wiadomośc potwierdzającą
      {
        logger.debug("0.999 devices has been processed -> sending 'done'...")
        systemSender ! "done"
      }
    case Failed(id : Int) =>
      logger.debug("'Failed' message received from processing devId: " + id)
      workerRouter ! DeviceIDProc(id)
    case _ =>
      //println("Unsupported message")
      logger.error("Unsupported message received.")
  }

  private def splitID(begin : Int, end : Int) : Unit = {
    for (id <- begin until end) {
      workerRouter ! DeviceIDProc(id)
    }
  }


}
