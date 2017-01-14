package pl.edu.pw.elka.devicematcher.utils

import java.util

import pl.edu.pw.elka.devicematcher.topicmodel.Document
import collection.JavaConversions._
/**
  * Created by lukier on 1/14/17.
  */
object MatcherDataTest {
  val docs = new util.LinkedList[Document]()
  val arr = Array[Document](
    new Document(0,new util.ArrayList[String](),new util.ArrayList[String](), new util.ArrayList[String]()),
    new Document(1,new util.ArrayList[String](),new util.ArrayList[String](),new util.ArrayList[String]()),
    new Document(2,new util.ArrayList[String](),new util.ArrayList[String](),new util.ArrayList[String]()),
    new Document(4,new util.ArrayList[String](),new util.ArrayList[String](),new util.ArrayList[String]()),
    new Document(5,new util.ArrayList[String](),new util.ArrayList[String](),new util.ArrayList[String]()),
    new Document(6,new util.ArrayList[String](),new util.ArrayList[String](),new util.ArrayList[String]()))
  docs.addAll(arr.toList)
}
