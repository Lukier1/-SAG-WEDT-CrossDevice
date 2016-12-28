package pl.edu.pw.elka.devicematcher

import pl.edu.pw.elka.devicematcher.utils.NLPUtils

object DeviceMatcherApp extends App {

  override def main(args: Array[String]) : Unit = {
//        val iterator = DeviceQueryDAO.getDeviceQueriesByDevice(1)
//        while(iterator.hasNext)
//          println(iterator.next())
//
//        Database.client.close()

    var content = "european union buildings of new york lorem ipsum paris a john has got microsoft josef something new york on tower brigde in london city"
    content = NLPUtils.markNamedEntities(content)
    println(content)
  }
}
