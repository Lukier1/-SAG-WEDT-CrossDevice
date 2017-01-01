package pl.edu.pw.elka.devicematcher

import java.util

import edu.stanford.nlp.ling.CoreLabel
import pl.edu.pw.elka.devicematcher.utils.NLPUtils

object DeviceMatcherApp extends App {

  override def main(args: Array[String]): Unit = {
    //        val iterator = DeviceQueryDAO.getDeviceQueriesByDevice(1)
    //        while(iterator.hasNext)
    //          println(iterator.next())
    //
    //        Database.client.close()

    /**
      * przyklad ddzialania procesu
      */
    var content = "\"Oh, no,\" she's saying, \"our $400 blender can't handle something this hard! Shops in new york are too fuck. London city has better shops.\""
    val nerResult = NLPUtils.markNamedEntities(content)
    val namedEntities = nerResult.subList(1, nerResult.size())
    val tokens = NLPUtils.tokenize(nerResult.get(0))
    var labels: util.List[CoreLabel] = NLPUtils.tagPartOfSpeech(tokens)
    labels = NLPUtils.lemmatize(labels)
    labels = NLPUtils.deleteStopWords(labels)
    println("##labels##")
    for (i <- 0 to labels.size()-1){
      println(i+". "+labels.get(i).word()+" : "+labels.get(i).tag()+" : "+labels.get(i).lemma())
    }
    println("##named enitites##")
    for (i <- 0 to namedEntities.size()-1){
      println(i+". "+namedEntities.get(i))
    }

  }
}
