package pl.edu.pw.elka.devicematcher.data

import java.util

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.client.model.Projections
import pl.edu.pw.elka.devicematcher.data.model.DeviceQueryDocument
import pl.edu.pw.elka.devicematcher.topicmodel.Document

import scala.collection.JavaConversions.seqAsJavaList

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
  private val DEVICE_QUERY_COLLECTION_NAME = "DeviceQuery"

  /**
    * Zwraca wszystkie deviceQuery z bazy
    * Obiekty mozna w szczegolnosci przekonwertowac na klase {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument}
    * za pomoca jej konstruktora {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument#this(DBObject)}
    *
    * @return iterator po wszystkich elementach w bazie
    */
  def getAllDevices(): Iterator[DBObject] = {
    val collection: MongoCollection = Database.db(DEVICE_QUERY_COLLECTION_NAME)
    val iterator = collection.find()
    iterator
  }

  /**
    * Zwraca wszystkie deviceQuery z bazy
    * Obiekty mozna w szczegolnosci przekonwertowac na klase {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument}
    * za pomoca jej konstruktora {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument#this(DBObject)}
    *
    * @param projectionFields projekcja kolumn
    * @return iterator po wszystkich elementach w bazie
    */
  def getAllDevices(projectionFields: Array[String]): Iterator[DBObject] = {
    val collection: MongoCollection = Database.db(DEVICE_QUERY_COLLECTION_NAME)
    val iterator = collection.find()
    var projection = MongoDBObject.newBuilder
    if (projectionFields.isEmpty)
      collection.find()
    else {
      for (field <- projectionFields)
        projection += (field -> 1)
      collection.find(MongoDBObject.empty, projection.result())
    }
  }

  /**
    * Zwraca wszystkie deviceQuery dla danego urzadzenia
    * Obiekty mozna w szczegolnosci przekonwertowac na klase {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument}
    * za pomoca jej konstruktora {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument#this(DBObject)}
    *
    * @param deviceId identyfikator urzadzenia
    * @return iterator po deviceQuery danego urzadzenia
    */
  def getDeviceQueriesByDevice(deviceId: Integer): Iterator[DBObject] = {
    val db = Database.db
    val collection: MongoCollection = Database.db(DEVICE_QUERY_COLLECTION_NAME)
    val criteria = MongoDBObject(Columns.DEVICE_ID -> deviceId)
    val iterator = collection.find(criteria)
    iterator
  }

  /**
    * Zwraca wszystkie deviceQuery dla danego urzadzenia.
    * Obiekty mozna w szczegolnosci przekonwertowac na klase {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument}
    * za pomoca jej konstruktora {@link pl.edu.pw.elka.devicematcher.data.model.#DeviceQueryDocument#this(DBObject)}
    *
    * @param deviceId         identyfikator urzadzenia
    * @param projectionFields projekcja kolumn
    * @return iterator po deviceQuery danego urzadzenia
    */
  def getDeviceQueriesByDevice(deviceId: Integer, projectionFields: Array[String]): Iterator[DBObject] = {
    val db = Database.db
    val collection: MongoCollection = Database.db(DEVICE_QUERY_COLLECTION_NAME)
    val criteria = MongoDBObject(Columns.DEVICE_ID -> deviceId)
    var projection = MongoDBObject.newBuilder
    if (projectionFields.isEmpty)
      collection.find(criteria)
    else {
      for (field <- projectionFields)
        projection += (field -> 1)
      collection.find(criteria, projection.result())
    }
  }
}
