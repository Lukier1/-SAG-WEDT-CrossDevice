package pl.edu.pw.elka.devicematcher.data

import java.util

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import pl.edu.pw.elka.devicematcher.topicmodel.Document

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import com.mongodb.casbah.Imports._

/**
  * Created by szymon on 06.01.17.
  */
object DocumentDAO {

  /**
    * Kolumny
    */
  object Columns {
    val DEVICE_ID = "deviceId"
    val NAMED_ENTITIES = "namedEntites"
    val WORDNET_TERMS = "wordnetTerms"
    val OTHER_TERMS = "otherTerms"
  }

  /**
    * Nazwa kolekcji
    */
  private val DOCUMENT_COLLECTION_NAME = "Document"

  /**
    * Dodaje dokument do bazy
    *
    * @param document dokument
    */
  def addDocument(document: Document): Unit = {
    val collection = Database.db(DOCUMENT_COLLECTION_NAME)

    val docObj = MongoDBObject(Columns.DEVICE_ID -> document.getDeviceID(),
      Columns.NAMED_ENTITIES -> getNamedEntitiesFromDocument(document),
      Columns.WORDNET_TERMS -> getWordnetTermsFromDocument(document),
      Columns.OTHER_TERMS -> getOtherTermsFromDocument(document))
    collection.insert(docObj)
  }

  /**
    * Dodaje dokumenty do bazy
    *
    * @param docs lista dokumentow
    */
  def addDocuments(docs: util.List[Document]): Unit = {
    val collection = Database.db(DOCUMENT_COLLECTION_NAME)
    val builder = collection.initializeUnorderedBulkOperation
    for (d <- docs) {
      val docObj = MongoDBObject(Columns.DEVICE_ID -> d.getDeviceID(),
        Columns.NAMED_ENTITIES -> getNamedEntitiesFromDocument(d),
        Columns.WORDNET_TERMS -> getWordnetTermsFromDocument(d),
        Columns.OTHER_TERMS -> getOtherTermsFromDocument(d))
      builder.insert(docObj)
    }
    builder.execute()
  }

  /**
    * Zwraca wszystkie dokumenty w bazie.
    * W szczegolnosci mozna je przetworzyc funkcja:
    * {@link #fromDBObjectToDocument(dBObject: DBObject)}
    *
    * @return iterator po wszystkich dokumentach w bazie
    */
  def getAllDocuments(): Iterator[DBObject] = {
    val collection: MongoCollection = Database.db(DOCUMENT_COLLECTION_NAME)
    val iterator = collection.find()
    iterator
  }

  /** Zwraca wszystkie dokumenty w bazie.
    * W szczegolnosci mozna je przetworzyc funkcja:
    * {@link #fromDBObjectToDocument(dBObject: DBObject)}
    *
    * @param projectionFields projekcja kolumn
    * @return iterator po wszystkich dokumentach w bazie
    */
  def getAllDocuments(projectionFields: Array[String]): Iterator[DBObject] = {
    val collection: MongoCollection = Database.db(DOCUMENT_COLLECTION_NAME)
    val projection = MongoDBObject.newBuilder
    if (projectionFields.isEmpty)
      collection.find()
    else {
      for (field <- projectionFields)
        projection += (field -> 1)
      collection.find(MongoDBObject.empty, projection.result())
    }
  }

  /**
    * Zwraca JEDEN! obiekt zawierający podany deviceId
    *
    * @param deviceId deviceId dokumentu
    * @return obiekt klasy dokument o podanym deviceId
    */
  def getDocumentByDevice(deviceId: Integer): Document = {
    val db = Database.db
    val collection: MongoCollection = Database.db(DOCUMENT_COLLECTION_NAME)
    val criteria = MongoDBObject(Columns.DEVICE_ID -> deviceId)
    val obj = collection.findOne(criteria).iterator.next()
    fromDBObjectToDocument(obj)
  }

  /**
    * Przetwarza obiekt zwrocony przez baze danych do obiektu klasy Document
    *
    * @param dBObject obiekt zwrocony przez baze danych
    * @return obiekt klasy Document
    */
  def fromDBObjectToDocument(dBObject: DBObject): Document = {
    var namedEntitiesArr: util.List[String] = seqAsJavaList(dBObject.get(Columns.NAMED_ENTITIES).toString.split(","))
    var wordnetTerms: util.List[String] = seqAsJavaList(dBObject.get(Columns.WORDNET_TERMS).toString.split(","))
    var otherTerms: util.List[String] = seqAsJavaList(dBObject.get(Columns.OTHER_TERMS).toString.split(","))
    new Document(Integer.parseInt(dBObject.get(Columns.DEVICE_ID).toString),
      namedEntitiesArr,
      wordnetTerms,
      otherTerms)
  }

  private def getNamedEntitiesFromDocument(document: Document): String = {
    document.getNamedEntities().asScala.toList.flatMap(Option[String]).filter(_ != null).mkString(",")
  }

  private def getWordnetTermsFromDocument(document: Document): String = {
    document.getWordnetTerms().asScala.toList.flatMap(Option[String]).filter(_ != null).mkString(",")
  }

  private def getOtherTermsFromDocument(document: Document): String = {
    document.getOtherTerms().asScala.toList.flatMap(Option[String]).filter(_ != null).mkString(",")
  }
}
