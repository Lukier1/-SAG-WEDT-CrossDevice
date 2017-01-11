package pl.edu.pw.elka.devicematcher.topicmodel

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

/**
  * Created by dawid on 11.01.17.
  */
object TopicModelSerializer {
  private val TOPIC_MODEL_DIR = "./src/main/resources/models/"

  /**
    * Serializacja i zapis danego modelu LDA do pliku.
    *
    * @param model model LDA do zapisu
    * @param filename nazwa pliku
    * @return true jeśli zapis powiódł się, false w p.p.
    */
  def writeTopicModelToFile(model: TopicModel, filename: String): Boolean = {
    try {
      val oos =  new ObjectOutputStream(new FileOutputStream(TOPIC_MODEL_DIR + filename))
      oos.writeObject(model)
      oos.close()
    } catch {
      case e: Exception =>
        return false
    }
    true
  }

  /**
    * Odczyt i deserializacja modelu LDA z pliku.
    *
    * @param filename nazwa pliku
    * @return model LDA lub null, gdy się nie powiedzie odczyt/deserializacja
    */
  def readTopicModelFromFile(filename: String): TopicModel = {
    try {
      val ois = new ObjectInputStream(new FileInputStream(TOPIC_MODEL_DIR + filename))
      val model = ois.readObject.asInstanceOf[TopicModel]
      ois.close()
      return model
    } catch {
      case e: Exception =>
        return null
    }
  }
}
