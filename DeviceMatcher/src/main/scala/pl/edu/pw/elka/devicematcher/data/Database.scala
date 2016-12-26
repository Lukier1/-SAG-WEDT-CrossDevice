package pl.edu.pw.elka.devicematcher.data

import com.mongodb.casbah.Imports._
/**
  * Created by szymon on 24.12.16.
  */
object Database {
  private val DATABASE_NAME = "database"

  val client = MongoClient("localhost", 27017)  //klasa klienta trzyma połączenie
  val db: MongoDB = client(DATABASE_NAME)
}
