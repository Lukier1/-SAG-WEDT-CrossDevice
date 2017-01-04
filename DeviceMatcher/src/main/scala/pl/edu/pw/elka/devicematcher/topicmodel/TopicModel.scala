package pl.edu.pw.elka.devicematcher.topicmodel

import java.util

import collection.JavaConversions._
import cc.mallet.pipe.iterator.ArrayIterator
import cc.mallet.pipe.{Pipe, SerialPipes, TokenSequence2FeatureSequence}
import cc.mallet.topics.ParallelTopicModel
import cc.mallet.types.{Alphabet, IDSorter, InstanceList, TokenSequence}

/**
  * Created by dawid on 02.01.17.
  */
class TopicModel(numOfTopics: Int, iterations: Int, a: Double, b: Double) {

  /**
    * Liczba tematow do zamodelowania i liczba przebiegow algorytmu LDA
    */
  private val topicsCount: Int = numOfTopics
  private val passes: Int = iterations

  /**
    * Liczba dokumentow, na ktorych trenowany byl model
    */
  private var docsCount: Int = _

  /**
    * Mapowanie deviceID (zawarte w dokumentach LDA) a identyfikatorami malletowych instancji trenujacych model LDA
    */
  private var dev2InstanceIDMap: util.HashMap[Int,Int] = _

  /**
    * Parametry modelu (rozkladu Dirichleta modelowanych tematow)
    */
  private val alpha: Double = a
  private val beta: Double = b

  /**
    * Malletowy model LDA
    */
  private var model: ParallelTopicModel = _

  /**
    * Trenuje model LDA na zadanym zbiorze dokumentow
    *
    * @param documents Lista dokumentow
    * @param withOtherTerms Flaga wlaczenia w modelowanie slow dokumentow ze zbioru 'pozostale'
    */
  def train(documents: util.List[Document], withOtherTerms: Boolean): Unit = {
    dev2InstanceIDMap = new util.HashMap[Int,Int]()
    docsCount = documents.size()

    val docs = new util.ArrayList[TokenSequence]()
    var i = 0
    for (d: Document <- documents) {
      docs.add(d.getDoc(withOtherTerms))
      dev2InstanceIDMap.put(d.getDeviceID(), i)
      i += 1
    }

    val pipeList = new util.ArrayList[Pipe]()
    pipeList.add(new TokenSequence2FeatureSequence());
    val instances = new InstanceList(new SerialPipes(pipeList));
    instances.addThruPipe(new ArrayIterator(docs));

    model = new ParallelTopicModel(topicsCount, alpha * topicsCount, beta);
    model.addInstances(instances)
    model.setNumIterations(iterations)
    model.estimate()
  }

  def getTopicsCount(): Int = { topicsCount }
  def getIterations(): Int = { passes }
  def getAlpha(): Double = { alpha }
  def getBeta(): Double = { beta }
  def getDocumentsCount(): Int = { docsCount }

  def getInstanceID(devID: Int): Int = {
    if (dev2InstanceIDMap == null) {
      return -1
    }
    dev2InstanceIDMap.get(devID)
  }

  /**
    * Zwraca rozklad tematow wyszukiwan dla dokumentu o zadanym deviceID
    *
    * @param devID
    * @return tablice wartosci p-nstw poszczegolnych tematow wyszukiwania
    */
  def getTopicDistributionByDevID(devID: Int): Array[Double] = {
    if (model == null || dev2InstanceIDMap == null) {
      return null
    }
    model.getTopicProbabilities(getInstanceID(devID))
  }

  /**
    * Zwraca rozklad tematow wyszukiwan dla instancji trenujacej o zadanym id
    *
    * @param instanceID
    * @return tablice wartosci p-nstw poszczegolnych tematow wyszukiwania
    */
  def getTopicDistributionByInstanceID(instanceID: Int): Array[Double] = {
    if (model == null) {
      return null
    }
    model.getTopicProbabilities(instanceID)
  }

  /**
    * Patrz: http://mallet.cs.umass.edu/api/cc/mallet/topics/ParallelTopicModel.html#getAlphabet()
    *        http://mallet.cs.umass.edu/api/cc/mallet/types/Alphabet.html
    *
    * @return
    */
  def getAlphabet(): Alphabet = {
    if (model == null) {
      return null
    }
    model.getAlphabet()
  }

  /**
    * Patrz: http://mallet.cs.umass.edu/api/cc/mallet/topics/ParallelTopicModel.html#getSortedWords()
    *
    * @return
    */
  def getTopicSortedWords(): util.ArrayList[util.TreeSet[IDSorter]] = {
    if (model == null) {
      return null
    }
    model.getSortedWords()
  }

}
