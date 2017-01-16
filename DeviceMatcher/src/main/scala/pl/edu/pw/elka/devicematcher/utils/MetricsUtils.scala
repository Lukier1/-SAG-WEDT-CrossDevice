package pl.edu.pw.elka.devicematcher.utils

import pl.edu.pw.elka.devicematcher.data.{AnonDeviceDAO, DeviceQueryDAO}

import scala.collection.JavaConversions._
import java.util

/**
  * Created by dawid on 02.01.17.
  */
object MetricsUtils {

  def log2(a: Double): Double = {
    Math.log(a) / Math.log(2)
  }

  /**
    * Obliczenie dywergencji Kullbacka-Leiblera dla zadanych dwoch rozkladow prawdopodobienstwa.
    *
    * @param p Pierwszy rozklad p-nstwa
    * @param q Drugi rozklad p-nstwa
    * @throws java.lang.IllegalArgumentException gdy rozklady p i q nie sa tego samego rozmiaru
    * @return Wartosc dywergencji (Double)
    */
  @throws(classOf[IllegalArgumentException])
  def divKL(p: Array[Float], q: Array[Float]): Double = {
    if (p.length != q.length)
      throw new IllegalArgumentException
    var div = 0.0
    for (i <- p.indices) {
      val qv = if (q(i) < 0.0000001f) 0.0000001f else q(i)
      val pv = if (p(i) < 0.0000001f) 0.0000001f else p(i)
      div += p(i) * log2(pv/qv)
    }
    div
  }

  /**
    * Obliczenie dywergencji Jensena-Shannona dla zadanych dwoch rozkladow prawdopodobienstwa.
    *
    * @param p Pierwszy rozklad p-nstwa
    * @param q Drugi rozklad p-nstwa
    * @throws java.lang.IllegalArgumentException gdy rozklady p i q nie sa tego samego rozmiaru
    * @return Wartosc dywergencji (Double)
    */
  @throws(classOf[IllegalArgumentException])
  def divJS(p: Array[Float], q: Array[Float]): Double = {
    if (p.length != q.length)
      throw new IllegalArgumentException
    val m = new Array[Float](p.length)
    for (i <- m.indices) {
      m(i) = (p(i)+q(i))/2.0f
    }
    val pm = divKL(p, m)
    val qm = divKL(q, m)
    (pm+qm)/2.0
  }

  /**
    * Obliczenie precyzji
    *
    * @param tp True Positive - "with hit"
    * @param fp False Positive - "false alarm"
    * @return precyzja (precision / positive predictive value / probability of detection)
    */
  def precision(tp: Int, fp: Int): Double = {
    val precision = tp.toDouble / (tp + fp)
    precision
  }

  /**
    * Obliczenie czułości / pokrycia
    *
    * @param tp True Positive - "with hit"
    * @param fn False Negative - "with miss"
    * @return czułość (recall / sensitivity / hit rate / true positive rate)
    */
  def recall(tp: Int, fn: Int): Double = {
    val recall = tp.toDouble / (tp + fn)
    recall
  }

  /**
    * Obliczenie specyficzności
    *
    * @param tn True Negative - "with correct rejection"
    * @param fp False Positive - "false alarm"
    * @return specyficzność (specificity / true negative rate)
    */
  def specificity(tn: Int, fp: Int): Double = {
    val specificity = tn.toDouble / (fp + tn)
    specificity
  }

  /**
    * Obliczenie dokładności
    *
    * @param tp True Positive - "with hit"
    * @param fp False Positive - "false alarm"
    * @param tn True Negative - "with correct rejection"
    * @param fn False Negative - "with miss"
    * @return dokładność (accuracy)
    */
  def accuracy(tp: Int, fp: Int, tn: Int, fn: Int): Double = {
    val accuracy = (tp + tn).toDouble / (tp + tn + fp + fn)
    accuracy
  }

  /**
    * Obliczenie współczynnika/miary F
    *
    * @param precision
    * @param recall
    * @return miara F (F-measure / F1-score / harmonic mean of precision and sensitivity)
    */
  def f_measure(precision: Double, recall: Double): Double = {
    val f = 2 * precision * recall / (precision + recall)
    f
  }

  /** Wylicza podstawowe metryki dopasowania.
    *
    * @param groups grupy przyporządkowane przez algorytm grupowania urządzeń
    * @return lista metryk, kolejno: positives, falsePositives, negatives, falseNegatives
    */
  def getBasicMetrics(groups: util.List[Group], startIndex:Int=0, range: Int=0): util.List[Int] = {
    val maxDeviceId = AnonDeviceDAO.getMaxDeviceId()
    val length = if (range == 0) maxDeviceId else range+startIndex
    if (length>maxDeviceId)
      throw new IllegalArgumentException("Range :"+length+" is too big for AnonDevices collection size: "+maxDeviceId)

    var falsePositives = 0
    var falseNegatives = 0
    var positives = 0
    var negatives = 0
    for (i <- startIndex until length) {
      val anonId = AnonDeviceDAO.getAnonIdForDevice(i)
      if (anonId != -1) {
        var group: Group = null
        for (g <- groups) {
          if (g.containsDevId(i) || g.devId == i) {
            group = g
          }
        }
        for (j <- startIndex until length if i != j && group!=null) {
          val otherAnonId = AnonDeviceDAO.getAnonIdForDevice(j)
          if (otherAnonId != -1) {
            val isInTheSameGroup = group.containsDevId(j) || group.devId == j
            val hasTheSameUser = anonId == AnonDeviceDAO.getAnonIdForDevice(j)
            if (isInTheSameGroup && !hasTheSameUser)
              falsePositives += 1
            else if (!isInTheSameGroup && hasTheSameUser)
              falseNegatives += 1
            else if (isInTheSameGroup && hasTheSameUser)
              positives += 1
            else if (!isInTheSameGroup && !hasTheSameUser)
              negatives += 1
          }
        }
      }
    }
    new util.LinkedList[Int](List(positives, falsePositives, negatives, falseNegatives))
  }

}
