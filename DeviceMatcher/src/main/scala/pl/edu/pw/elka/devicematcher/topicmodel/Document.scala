package pl.edu.pw.elka.devicematcher.topicmodel

import java.util

import scala.collection.JavaConversions._

import cc.mallet.types.TokenSequence

/**
  * Created by dawid on 02.01.17.
  */
class Document(devID: Int, namedEnts: util.List[String], wordnetWords: util.List[String],
               otherWords: util.List[String]) {
  private val deviceID: Int = devID

  /**
    * Zbiory: 'encje nazwane', 'pojecia', 'pozostale slowa'
    */
  private val namedEntities = new util.ArrayList[String]()
  namedEntities.addAll(namedEnts)
  private val wordnetTerms = new util.ArrayList[String]()
  wordnetTerms.addAll(wordnetWords)
  private val otherTerms = new util.ArrayList[String]()
    otherTerms.addAll(otherWords)

  /**
    * Rozklad wystepowania tematow wyszukiwan
    */
  private var topicDistribution: Array[Double] = _

  def getDeviceID() = deviceID

  def addToNamedEntities(words: util.Collection[String]): Unit = {
    namedEntities.addAll(words)
  }
  def addToWordnetTerms(words: util.Collection[String]): Unit = {
    wordnetTerms.addAll(words)
  }
  def addToOtherTerms(words: util.Collection[String]): Unit = {
    otherTerms.addAll(words)
  }

  def getNamedEntities(): util.List[String] = { namedEntities }
  def getWordnetTerms(): util.List[String] = { wordnetTerms }
  def getOtherTerms(): util.List[String] = { otherTerms }

  def namedEntitiesCount(): Int = { namedEntities.size() }
  def wordnetTermsCount(): Int = { wordnetTerms.size() }
  def otherTermsCount(): Int = { otherTerms.size() }

  /**
    * Tworzy sekwencji tokenow (w formie strawnej dla Malleta) z przechowywanych
    * termow ze zbiorow: 'encje nazwane', 'pojecia' i ew. 'pozostale slowa'
    *
    * @param withOtherTerms Flaga wlaczenia w sekwencje tokenow termow z 'pozostale slowa'
    * @return Malletowa sekwencja tokenow (TokenSequence)
    */
  def getDoc(withOtherTerms: Boolean): TokenSequence = {
    val seq = new TokenSequence()
    for (w <- namedEntities) {
      seq.add(w)
    }
    for (w <- wordnetTerms) {
      seq.add(w)
    }
    if (withOtherTerms) {
      for (w <- otherTerms) {
        seq.add(w)
      }
    }
    seq
  }

  def setTopicDistribution(dist: Array[Double]): Unit = {
    topicDistribution = dist
  }
  def getTopicDistribution(): Array[Double] = { topicDistribution }

  override def toString(): String = {
    val str = new StringBuilder("devID: " + devID + " named_entities: { ")
    for (w <- namedEntities) {
      str.append(w + ",")
    }
    str.append(" } wordnetTerms: { ");
    for (w <- wordnetTerms) {
      str.append(w + ",")
    }
    str.append(" } otherTerms: { ");
    for (w <- otherTerms) {
      str.append(w + ",")
    }
    str.append(" }");
    str.toString()
  }

}
