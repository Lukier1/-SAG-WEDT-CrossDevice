package pl.edu.pw.elka.devicematcher.utils

import java.net.URL
import java.util

import scala.collection.JavaConversions._

import edu.mit.jwi.Dictionary
import edu.mit.jwi.IDictionary
import edu.mit.jwi.item._

/**
  * Created by dawid on 02.01.17.
  */
object WordnetUtils {

  private val url = WordnetUtils.getClass().getResource("/wordnet/dict")

  /**
    * Tworzy slownik WordNet.
    *
    * @return Nieotwarty slownik WordNet
    */
  def getDictionary(): IDictionary = {
    val dict = new Dictionary(url)
    dict
  }

  /**
    * Sprawdza czy dana lemma jest w WordNecie.
    *
    * @param dict Otwarty slownik WordNet
    * @param lemma
    * @return true jesli istnieje, false w.p.p.
    */
  def existsInWordnet(dict: IDictionary, lemma: String): Boolean = {
    try {
      var idxWord = dict.getIndexWord(lemma, POS.NOUN)
      if (idxWord != null)
        return true
      idxWord = dict.getIndexWord(lemma, POS.ADJECTIVE)
      if (idxWord != null)
        return true
      idxWord = dict.getIndexWord(lemma, POS.ADVERB)
      if (idxWord != null)
        return true
      idxWord = dict.getIndexWord(lemma, POS.VERB)
      if (idxWord != null)
        return true
      false
    } catch {
      case iae: IllegalArgumentException =>
        false
      case e: Exception =>
        throw e
    }
  }

  /**
    * Wyszukuje w slowniku synonimow dla danej lemmy.
    *
    * @param dict Otwarty slownik WordNet
    * @param lemma Lemma, ktorej synonimow poszukujemy
    * @param tag WordNetowy POS tag lemmy
    * @param meaningDepth Maksymalna liczba roznych znaczen danej lemmy do rozpatrzenia
    * @param useSpaces Flaga uzycia spacji jako separatora w synonimach. Niektore synonimy moga skladac sie z wiecej
    *                  niz 1 slowa - domyslnym separatorem w WordNecie jest '_'
    * @return Lista synonimow (String)
    */
  def getSynonyms(dict: IDictionary, lemma: String, tag: POS, meaningDepth: Int, useSpaces: Boolean): util.List[String] = {
    val syns = new util.LinkedHashSet[String]()
    val synonyms = new util.ArrayList[String]()
    val idxWord = dict.getIndexWord(lemma, POS.NOUN)
    if (idxWord == null)
      return synonyms

    val wordIDs = idxWord.getWordIDs()
    for (i <- 0 to wordIDs.size() if i < meaningDepth) {
      val wordID = idxWord.getWordIDs().get(i)
      val word = dict.getWord(wordID)
      val synset = word.getSynset()
      for (w: IWord <- synset.getWords()) {
        var s = w.getLemma()
        if (useSpaces) {
          s = s.replace("_", " ")
        }
        syns.add(s)
      }
    }

    synonyms.addAll(syns)
    synonyms
  }

  /**
    * Wyszukuje w slowniku hiperonimow dla danej lemmy.
    *
    * @param dict Otwarty slownik WordNet
    * @param lemma Lemma, ktorej hiperonimow poszukujemy
    * @param tag WordNetowy POS tag lemmy
    * @param meaningDepth Maksymalna liczba roznych znaczen danej lemmy do rozpatrzenia
    * @param useSpaces Flaga uzycia spacji jako separatora w synonimach. Niektore synonimy moga skladac sie z wiecej
    *                  niz 1 slowa - domyslnym separatorem w WordNecie jest '_'
    * @return Lista hiperonimow (String)
    */
  def getHypernyms(dict: IDictionary, lemma: String, tag: POS, meaningDepth: Int, useSpaces: Boolean): util.List[String] = {
    val hips = new util.LinkedHashSet[String]()
    val hypernyms = new util.ArrayList[String]()
    val idxWord = dict.getIndexWord(lemma, POS.NOUN)
    if (idxWord == null)
      return hypernyms

    val wordIDs = idxWord.getWordIDs();
    for (i <- 0 to wordIDs.size() if i < meaningDepth) {
      val wordID = idxWord.getWordIDs().get(i)
      val word = dict.getWord(wordID)
      val synset = word.getSynset()
      val h = synset.getRelatedSynsets(Pointer.HYPERNYM)
      for (sid: ISynsetID <- h) {
        val words = dict.getSynset(sid).getWords()
        for (w: IWord <- words) {
          var s = w.getLemma()
          if (useSpaces) {
            s = s.replace("_", " ")
          }
          hips.add(s)
        }
      }
    }

    hypernyms.addAll(hips)
    hypernyms
  }

  /**
    * Wyodrebnia te slowa w zadanej liscie slow, ktore sa pojeciami zidentyfikowanymi przez WordNet.
    *
    * @param dict Otwarty slownik WordNet
    * @param words Lista slow do zbadania przez slownik
    * @return Lista pojec zidentyfikowanych przez slownik WordNet
    */
  def retrieveWordnetTerms(dict: IDictionary, words: util.Collection[String]): util.List[String] = {
    val terms = new util.ArrayList[String]()
    for (w <- words) {
      if (existsInWordnet(dict, w))
        terms.add(w)
    }
    terms
  }

}
