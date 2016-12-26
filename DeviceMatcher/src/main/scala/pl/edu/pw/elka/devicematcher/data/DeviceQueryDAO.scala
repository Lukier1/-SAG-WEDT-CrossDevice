package pl.edu.pw.elka.devicematcher.data

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject

/**
  * Created by szymon on 25.12.16.
  */
object DeviceQueryDAO {

  /**
    * Kolumny kolekcji
    */
  object Columns {
    val DEVICE_ID = "DeviceId"
    val ANON_ID = "AnonId"
    val QUERY = "Query"
  }

  /**
    * Nazwa kolekcji
    */
  val DEVICE_QUERY_COLLECTION_NAME = "DeviceQuery"


  def getAllDevices(): Iterator[DBObject] = {
    val collection: MongoCollection = Database.db(DEVICE_QUERY_COLLECTION_NAME)
    val iterator = collection.find().toIterator
    iterator
  }

  def getDeviceQueriesByDevice(deviceId :Integer): Iterator[DBObject] = {
    val db = Database.db
    val collection: MongoCollection = db(DEVICE_QUERY_COLLECTION_NAME)
    val criteria = MongoDBObject("DeviceId" -> deviceId)
    val iterator = collection.find(criteria).toIterator
    iterator
  }
}
