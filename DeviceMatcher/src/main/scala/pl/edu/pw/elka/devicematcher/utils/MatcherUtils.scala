package pl.edu.pw.elka.devicematcher.utils

import java.io.{FileOutputStream, FileWriter, ObjectOutputStream, PrintWriter}
import java.util
import scala.collection.JavaConversions._
import pl.edu.pw.elka.devicematcher.topicmodel.Document

/**
  * Created by szymon on 07.01.17.
  */
object MatcherUtils {

  /** Zwraca grupy zawierające maksymalnie @groupMembersSize elementow.
    * DevID urządzeń mogą występować w kilku różnych grupach na raz.
    * Wyszukiwanie grup jest niezależne od siebie dla wielu aktorów.
    *
    * @param docs             lista dokumentów
    * @param threshold        próg dywergencji powyżej którego dokumenty nie są ze sobą powiązane
    * @param startIndex       indeks elementu w liście dokumentów od którego funkcja ma zacząć tworzenie grup, domyślnie 0
    * @param range            obszar który po dodaniu do indeksu el. początkowego tworzy zakres przetwarzania, domyślnie rozmiar listy dokumentów
    * @param groupMembersSize ilość deviceID w każdej grupie nielicząc ID który indeksuje grupę, domyślnie 3 <- czyli jedno id główne + trzy id inne
    * @return lista grup w których niektóre devID mogę występować w wielu grupach
    */
  def getUntrimmedGroups(docs: util.List[Document], threshold: Double, startIndex: Int = 0, range: Int = -1, groupMembersSize: Int = 3): util.List[Group] = {

    val length = if (range == -1) docs.size() else range+startIndex
    if (length>docs.size())
      throw new IllegalArgumentException("Range :"+length+" is too big for document list size: "+docs.size())

    val groups = new util.LinkedList[Group]()
    var cacheGroup: Group = new Group(-1)
    for (i <- startIndex until length) {

      cacheGroup = new Group(docs.get(i).getDeviceID())
      if (cacheGroup.devId != -1)
        groups.add(cacheGroup)

      var tmpThreshold = threshold
      for (j <- 0 until docs.size() if docs(j).getDeviceID() != docs(i).getDeviceID()) {
        val div = MetricsUtils.divJS(docs.get(i).getTopicDistribution(), docs.get(j).getTopicDistribution())
        //val div = FakeJSDivergence.divJS(docs.get(i).getDeviceID(), docs.get(j).getDeviceID())
        if (div < tmpThreshold) {
          val thr = insertToSortedList(cacheGroup.members, new DocumentPair(docs.get(j).getDeviceID(), div), groupMembersSize)
          if (thr != 1.0)
            tmpThreshold = thr
        }
      }
    }
    groups
  }

  /** Usuwa powtarzające się devID z grup na podstawie dywergencji.
    *
    * @param threshold        próg dywergencji powyżej którego dokumenty nie są ze sobą powiązane
    * @param groups           grupy w których dany devID może być w kilku grupach
    * @param groupMembersSize ilość deviceID w każdej grupie nielicząc ID który indeksuje grupę, domyślnie 3 <- czyli jedno id główne + trzy id inne
    * @return lista grup w których każdy devID pojawia się tylko raz
    */
  def trimGroups(threshold: Double, groups: util.List[Group], groupMembersSize: Int = 3): util.List[Group] = {

    for (g <- groups) {
      var minDiv = g.minDivergence()
      var tmpDevID = g.devId
      for (ig <- groups if g.devId != ig.devId) {
        if (ig.containsDevId(g.devId)) {
          val tmpDiv = ig.divFor(g.devId)
          if (tmpDiv != Double.NaN && tmpDiv < minDiv && !ig.del) {
            minDiv = tmpDiv
            tmpDevID = ig.devId
          }
          ig.removeMember(g.devId)
        }
      }

      if (tmpDevID != g.devId) {
        g.del = true
        for (ig <- groups if g.devId != ig.devId) {
          for (dp <- g.members) {
            if (dp.devId == ig.devId || ig.containsDevId(g.devId)) {
              ig.del = false
            }
          }
          if (ig.devId == tmpDevID)
            insertToSortedList(ig.members, new DocumentPair(g.devId, minDiv), groupMembersSize)
        }
      }
    }

    for (g <- groups if !g.del) yield g
  }

  /** Zapisuje grupy do pliku
    *
    * @param list     grupy
    * @param filename nazwa pliku
    * @return true jesli zapis się powiódł
    */
  def writeGroupsToFile(list: util.List[Group], filename: String, trimmed: Boolean = true): Boolean = {
    try {
      val path = if(trimmed) "./src/main/resources/reports/groups/trimmed/" else "./src/main/resources/reports/groups/untrimmed/"
      val writer = new PrintWriter(new FileOutputStream(path + filename))
      for (l: Group <- list) {
        writer.print("G: " + l.devId + "\t[ " + l.devId + ", ")
        for (i <- 0 until l.members.size()) {
          writer.print(l.members.get(i).devId)
          if (i != l.members.size() - 1) {
            writer.print(", ")
          }
        }
        writer.print("]")
        for (i <- 0 to (3 - l.members.size()) * 2)
          writer.print(" ")
        writer.println(" deleted: " + l.del)

      }
      writer.close()
      return true
    } catch {
      case e: Exception =>
        return false
    }
  }

  /** Dodaje element do posortowanej listy w kolejności od elementu z najniższą dywergencją do największej.
    * Jeśli przez wartość dywergencji element ma być dodany po za zakresem @groupMembersSize to nie jest dodawany.
    *
    * @param list             lista członków grupy
    * @param pair             członek grupy do dodania
    * @param groupMembersSize maksymalny rozmiar grupy
    * @return największa dywergencja na liście
    */
  private def insertToSortedList(list: util.List[DocumentPair], pair: DocumentPair, groupMembersSize: Int = 3): Double = {
    if (list.size() == 0) {
      list.add(pair)
      return 1.0
    }
    for (i <- 0 until groupMembersSize) {
      if (i == list.size() || pair.div < list.get(i).div) {
        list.add(i, pair)
        while (list.size() > groupMembersSize)
          list.remove(list.size() - 1)

        if (list.size() < groupMembersSize)
          return 1.0
        else
          return list.get(list.size() - 1).div
      }
    }
    if (list.size() < groupMembersSize)
      1.0
    else
      list.get(list.size() - 1).div
  }

  private def printGroups(groups: util.List[Group]): Unit = {
    for (l: Group <- groups) {
      print("G: " + l.devId + "\t[ " + l.devId + ", ")
      for (i <- 0 until l.members.size()) {
        print("(" + l.members.get(i).devId + ", " + l.members.get(i).div + ")")
        if (i != l.members.size() - 1) {
          print(", ")
        }
      }
      println("]")
    }
  }
}

class DocumentPair(deviceId: Int, divergence: Double) {
  val devId: Int = deviceId
  val div: Double = divergence
}

/** Klasa reprezentująca grupę urządzeń skojarzonych za pomocą wartości dywergencji.
  *
  * @param deviceId indeks grupy
  * @param deleted  czy grupa jest usunięta
  */
class Group(deviceId: Int, deleted: Boolean = false) {
  val devId: Int = deviceId
  val members: util.List[DocumentPair] = new util.LinkedList[DocumentPair]()
  var del = deleted

  override def toString(): String = {
    var ret = "Group: devId: " + devId + ", members:["
    for (m <- members) {
      ret += ("," + m.devId.toString)
    }
    ret += "]"
    ret
  }

  /** Czy grupa zawiera podany devID na liście członków - nie tyczy się indeksu grupy!
    *
    * @param device devID szukanego urządzenia
    * @return Czy grupa zawiera podany devID
    */
  def containsDevId(device: Int): Boolean = {
    if (devId == device)
      return true
    for (m <- members) {
      if (m.devId == device)
        return true
    }
    false
  }

  /** Zwraca najmniejszą dywergencję pomiędzy indeksem grupy a jego członkami.
    *
    * @return 1.0 dla pustej listy członków, dla nie pustej minimalna dywergencja na liście
    */
  def minDivergence(): Double = {
    var min = 1.0
    for (dp <- members) {
      if (dp.div < min) {
        min = dp.div
      }
    }
    min
  }

  /** Zwraca dywergencję pomiędzy indeksem grupy i podanym devID.
    *
    * @param iDevID
    * @return dywergencja, lub  NotANumber jeśli devID nie jest na liście członków
    */
  def divFor(iDevID: Int): Double = {
    for (dp <- members) {
      if (dp.devId == iDevID) {
        return dp.div
      }
    }
    return Double.NaN
  }

  /** Usuwa urządzenie z listy członków.
    *
    * @param iDevID
    * @return true jeśli urządzenie zostaje usunięte z listy, false gdy urządzenie nie było członkiem
    */
  def removeMember(iDevID: Int): Boolean = {
    for (i <- 0 until members.size()) {
      if (members(i).devId == iDevID) {
        members.remove(i)
        return true
      }
    }
    false
  }
}

object FakeJSDivergence {
  var isMetrics = false
  private val divTable: Array[Array[Double]] = Array(
    //             q:    0     1     2     3     4     5     6    //p:
    Array[Double](/*0*/ 1.0,  1.0,  1.0,  1.0,  1.0, 1.0, 1.0), //0
    Array[Double](/*1*/ 1.0,  1.0,  1.0,  1.0,  1.0, 1.0, 1.0), //1
    Array[Double](/*2*/ 0.25, 1.0,  1.0,  1.0,  1.0, 1.0, 1.0), //2
    Array[Double](/*3*/ 1.0,  0.34, 1.0,  1.0,  1.0, 1.0, 1.0), //3
    Array[Double](/*4*/ 0.32, 0.33, 0.29, 1.0,  1.0, 1.0, 1.0), //4
    Array[Double](/*5*/ 0.21, 0.15, 1.0,  1.0,  1.0, 1.0, 1.0), //5
    Array[Double](/*6*/ 0.38, 0.11, 1.0,  0.26, 1.0, 1.0, 1.0) //6
    //             q:    0     1     2     3     4     5     6    //p^
  )

  def divJS(p: Int, q: Int): Double = {
    val p1 = if(isMetrics) p%9 else p
    val q1 = if(isMetrics) q%9 else q
    val ret = if (p1 >= q1) divTable(p1)(q1) else divTable(q1)(p1)
    ret
  }
}

