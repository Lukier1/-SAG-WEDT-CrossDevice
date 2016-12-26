package pl.edu.pw.elka.devicematcher.data.model

import com.mongodb.DBObject
import pl.edu.pw.elka.devicematcher.data.DeviceQueryDAO

/**
  * Created by szymon on 25.12.16.
  *
  * Ta klasa jest nieużywana
  * Jeśli zdecydujemy że nie będziemy pracować na DBObject to się nada
  */
class DeviceQueryDocument(deviceId: Integer, anonId: Integer, query: String) {

  def this(dBObject: DBObject) = {
    this(Option(dBObject.get(DeviceQueryDAO.Columns.DEVICE_ID).toString.toInt).get,
      Option(dBObject.get(DeviceQueryDAO.Columns.ANON_ID).toString.toInt).get,
      Option(dBObject.get(DeviceQueryDAO.Columns.QUERY).toString).get)
  }

  override def toString: String = {
    "AolDevicesRow { DeviceId : " + deviceId + ", AnonId : " + anonId + ", Query : " + query + " }"
  }
}
