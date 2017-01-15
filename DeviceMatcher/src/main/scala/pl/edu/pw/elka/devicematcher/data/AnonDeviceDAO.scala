package pl.edu.pw.elka.devicematcher.data

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject

/**
  * Created by szymon on 14.01.17.
  */
object AnonDeviceDAO {

  /**
    * Kolumny kolekcji
    */
  object Columns {
    val DEVICE_ID = "_id"
    val ANON_ID = "AnonId"
  }

  /**
    * Nazwa kolekcji
    */
  private val ANON_DEVICE_COLLECTION_NAME = "AnonDevice"

  /** Zwraca id użytkownika dla danego urządzenia
    *
    * @param deviceId
    * @return
    */
  def getAnonIdForDevice(deviceId: Integer): Integer = {
    val collection: MongoCollection = Database.db(ANON_DEVICE_COLLECTION_NAME)
    val criteria = MongoDBObject(Columns.DEVICE_ID -> deviceId)
    val iterator = collection.findOne(criteria)
    if (iterator.isEmpty)
      return -1
    val obj = collection.findOne(criteria).iterator.next()
    obj.get(Columns.ANON_ID).toString.toInt
  }

  def getCount(): Int = {
    val collection: MongoCollection = Database.db(ANON_DEVICE_COLLECTION_NAME)
    collection.count()
  }

  def getMaxDeviceId(): Int = {
    val collection: MongoCollection = Database.db(ANON_DEVICE_COLLECTION_NAME)
    val query = MongoDBObject()
    val fields = MongoDBObject("_id" -> 1)
    val orderBy = MongoDBObject("_id" -> -1)

    val it = collection.findOne(query, fields, orderBy).iterator
    if (it.nonEmpty) {
      return it.next.get(Columns.DEVICE_ID).toString.toInt
    }
    -1
  }
}
