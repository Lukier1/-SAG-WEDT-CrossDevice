package pl.edu.pw.elka.devicematcher.utils

import scala.collection.JavaConversions._

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
  def divKL(p: Array[Double], q: Array[Double]): Double = {
    if (p.length != q.length)
      throw new IllegalArgumentException
    var div = 0.0
    for (i <- p.indices) {
      val qv = if (q(i) < 0.0000001) 0.0000001 else q(i)
      val pv = if (p(i) < 0.0000001) 0.0000001 else p(i)
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
  def divJS(p: Array[Double], q: Array[Double]): Double = {
    if (p.length != q.length)
      throw new IllegalArgumentException
    val m = new Array[Double](p.length)
    for (i <- m.indices) {
      m(i) = (p(i)+q(i))/2.0
    }
    val pm = divKL(p, m)
    val qm = divKL(q, m)
    (pm+qm)/2.0
  }

}
