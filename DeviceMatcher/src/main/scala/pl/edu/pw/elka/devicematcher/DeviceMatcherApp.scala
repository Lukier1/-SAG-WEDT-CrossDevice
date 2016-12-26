package pl.edu.pw.elka.devicematcher

import pl.edu.pw.elka.devicematcher.data.{Database, DeviceQueryDAO}

object DeviceMatcherApp extends App{

  override def main(args: Array[String]) = {
    val iterator = DeviceQueryDAO.getDeviceQueriesByDevice(1)
    while(iterator.hasNext)
      println(iterator.next())

    Database.client.close()
  }
}
