package org.apache.spark.mllib.classification

import java.io._

import breeze.linalg.{DenseVector => BDV}

import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors}
import org.apache.spark.mllib.optimization.Gradient

import scala.util.Random

/**
  * Created by vincent on 16-12-19.
  */
/**
  *
  * @param numFeatures
  * @param numFields
  * @param param
  */
class FFMModel(numFeatures: Int,
               numFields: Int,
               param: FFMParameter,
               weights: Array[Double],
               sgd: Boolean = true ) extends Serializable {

  private var n: Int = numFeatures
  //numFeatures
  private var m: Int = numFields
  //numFields
  private var k: Int = param.k
  //numFactors
  private var normalization: Boolean = param.normalization
  private var initMean: Double = 0
  private var initStd: Double = 0.01

  require(n > 0 && k > 0 && m > 0)

  def radomization(l: Int, rand: Boolean): Array[Int] = {
    val order = Array.fill(l)(0)
    for (i <- 0 to l - 1) {
      order(i) = i
    }
    if (rand) {
      val rand = new Random()
      for (i <- l - 1 to 1) {
        val tmp = order(i - 1)
        val index = rand.nextInt(i)
        order(i - 1) = order(index)
        order(index) = tmp
      }
    }
    return order
  }

  def setOptimizer(op: String): Boolean = {
    if("sgd" == op) true else false
  }

  def predict(data: Array[(Int, Int, Double)], r: Double = 1.0): Double = {
    var t = 0.0
    val (align0, align1) = if(sgd) {
      (k, m * k)
    } else {
      (k * 2, m * k * 2)
    }

    // j: feature, f: field, v: value
    val valueSize = data.size //feature length
    val indicesArray = data.map(_._2) //feature index
    val valueArray: Array[(Int, Double)] = data.map(x => (x._1, x._3))
    var i = 0
    var ii = 0
    // j: feature, f: field, v: value
    while (i < valueSize) {
      val j1 = data(i)._2
      val f1 = data(i)._1
      val v1 = data(i)._3
      ii = i + 1
      if (j1 < n && f1 < m) {
        while (ii < valueSize) {
          val j2 = data(ii)._2
          val f2 = data(ii)._1
          val v2 = data(ii)._3
          if (j2 < n && f2 < m) {
            val w1_index: Int = j1 * align1 + f2 * align0
            val w2_index: Int = j2 * align1 + f1 * align0
            val v: Double = v1 * v2 * r
            for (d <- 0 to k - 1) {
              t += weights(w1_index + d) * weights(w2_index + d) * v
            }
          }
          ii += 1
        }
      }
      i += 1
    }
    /*
    for(n1 <- 0 to data.size - 1; n2 <- n1 + 1 to data.size - 1) {
      val j1 = data(n1)._2
      val f1 = data(n1)._1
      val v1 = data(n1)._3
      val j2 = data(n2)._2
      val f2 = data(n2)._1
      val v2 = data(n2)._3
      if(j1 < n && f1 < m && j2 < n && f2 < m) {
        val w1_index: Int = j1 * align1 + f2 * align0
        val w2_index: Int = j2 * align1 + f1 * align0
        val v: Double = v1 * v2 * r
        for (d <- 0 to k - 1) {
          t += weights(w1_index + d) * weights(w2_index + d) * v
        }
      }
    }
    */
    t
  }
}

class FFMGradient(m: Int, n: Int, k: Int, sgd: Boolean = true) extends Gradient {

  private def predict (data: Array[(Int, Int, Double)], weights: Array[Double], r: Double = 1.0): Double = {
    var t = 0.0
    val (align0, align1) = if(sgd) {
      (k, m * k)
    } else {
      (k * 2, m * k * 2)
    }

    //////rework data structure
    //    val labelVec = Vectors.sparse(data2.size, data2.map(_._2), data2.map(_._3))
    //    val fieldVec = Vectors.sparse(data2.size, data2.map(_._2), data2.map(_._1.toDouble))

    val valueSize = data.size //feature length
    val indicesArray = data.map(_._2) //feature index
    val valueArray: Array[(Int, Double)] = data.map(x => (x._1, x._3))
    var i = 0
    var ii = 0
    val a = data.size
    val b = indicesArray.length
    val c = valueArray.length
    val tt = 0
    // j: feature, f: field, v: value
    while (i < valueSize) {
      val j1 = data(i)._2
      val f1 = data(i)._1
      val v1 = data(i)._3
      ii = i + 1
      if (j1 < n && f1 < m) {
        while (ii < valueSize) {
        val j2 = data(ii)._2
        val f2 = data(ii)._1
        val v2 = data(ii)._3
        if (j2 < n && f2 < m) {
          val w1_index: Int = j1 * align1 + f2 * align0
          val w2_index: Int = j2 * align1 + f1 * align0
          val v: Double = v1 * v2 * r
          for (d <- 0 to k - 1) {
            t += weights(w1_index + d) * weights(w2_index + d) * v
          }
        }
        ii += 1
      }
    }
      i += 1
    }
    /////rework
    /*
    for(n1 <- 0 to data.size - 1; n2 <- n1 + 1 to data.size - 1) {
      val j1 = data(n1)._2
      val f1 = data(n1)._1
      val v1 = data(n1)._3
      val j2 = data(n2)._2
      val f2 = data(n2)._1
      val v2 = data(n2)._3
      if(j1 < n && f1 < m && j2 < n && f2 < m) {
        val w1_index: Int = j1 * align1 + f2 * align0
        val w2_index: Int = j2 * align1 + f1 * align0
        val v: Double = 2.0 * v1 * v2 * r
        for (d <- 0 to k - 1) {
          t += weights(w1_index + d) * weights(w2_index + d) * v
        }
      }
    }
    */
    t
  }

  override def compute(data: Vector, label: Double, weights: Vector): (Vector, Double) = {
    throw new Exception("This part is merged into computeFFM()")
  }

  override def compute(data: Vector, label: Double, weights: Vector, cumGradient: Vector): Double = {
    throw new Exception("This part is merged into computeFFM()")
  }
  def computeFFM(label: Double, data2: Array[(Int, Int, Double)], weights: Vector,
                 r: Double = 1.0, eta: Double, lambda: Double,
                 do_update: Boolean, iter: Int, solver: Boolean = true): (BDV[Double], Double) = {
 //   val data = data2.toVector
    // data2: [field, feature, value]
    val weightsArray: Array[Double] = weights.asInstanceOf[DenseVector].values
    val t = predict(data2, weightsArray, r)
    val expnyt = math.exp(-label * t)
    val tr_loss = math.log(1 + expnyt)
    val kappa = -label * expnyt / (1 + expnyt)
    val (align0, align1) = if(sgd) {
      (k, m * k)
    } else {
      (k * 2, m * k * 2)
    }
    var tt = 0
    //////rework data structure
//    val labelVec = Vectors.sparse(data2.size, data2.map(_._2), data2.map(_._3))
//    val fieldVec = Vectors.sparse(data2.size, data2.map(_._2), data2.map(_._1.toDouble))

    val valueSize = data2.size //feature length
    val indicesArray = data2.map(_._2) //feature index
    val valueArray: Array[(Int, Double)] = data2.map(x => (x._1, x._3))
    var i = 0
    var ii = 0

    // j: feature, f: field, v: value
    while (i < valueSize) {
      val j1 = data2(i)._2
      val f1 = data2(i)._1
      val v1 = data2(i)._3
      if (j1 < n && f1 < m) {
        ii = i + 1
        while (ii < valueSize) {
          val j2 = data2(ii)._2
          val f2 = data2(ii)._1
          val v2 = data2(ii)._3
          if (j2 < n && f2 < m) {
            val w1_index: Int = j1 * align1 + f2 * align0
            val w2_index: Int = j2 * align1 + f1 * align0
            val v: Double = v1 * v2 * r
            val wg1_index: Int = w1_index + k
            val wg2_index: Int = w2_index + k
            val kappav: Double = kappa * v
            for (d <- 0 to k - 1) {
              val g1: Double = lambda * weightsArray(w1_index + d) + kappav * weightsArray(w2_index + d)
              val g2: Double = lambda * weightsArray(w2_index + d) + kappav * weightsArray(w1_index + d)
              if (sgd) {
                weightsArray(w1_index + d) -= eta * g1
                weightsArray(w2_index + d) -= eta * g2
              } else {
                val wg1: Double = weightsArray(wg1_index + d) + g1 * g1
                val wg2: Double = weightsArray(wg2_index + d) + g2 * g2
                weightsArray(w1_index + d) -= eta / (math.sqrt(wg1)) * g1
                weightsArray(w2_index + d) -= eta / (math.sqrt(wg2)) * g2
                weightsArray(wg1_index + d) = wg1
                weightsArray(wg2_index + d) = wg2

              }
              tt += 1
            }
          }
          ii += 1
        }
      }
      i += 1
    }
    /////rework

/*
    for(n1 <- 0 to data2.size - 1; n2 <- n1 + 1 to data2.size - 1) {
      val j1 = data2(n1)._2
      val f1 = data2(n1)._1
      val v1 = data2(n1)._3
      val j2 = data2(n2)._2
      val f2 = data2(n2)._1
      val v2 = data2(n2)._3
      if(j1 < n && f1 < m && j2 < n && f2 < m) {
        val w1_index: Int = j1 * align1 + f2 * align0
        val w2_index: Int = j2 * align1 + f1 * align0
        val v: Double = 2.0 * v1 * v2 * r
        val wg1_index: Int = w1_index + k
        val wg2_index: Int = w2_index + k
        val kappav: Double = kappa * v
        for(d <- 0 to k-1) {
          val g1: Double = lambda * weightsArray(w1_index + d) + kappav * weightsArray(w2e32q_index + d)
          val g2: Double = lambda * weightsArray(w2_index + d) + kappav * weightsArray(w1_index + d)
          if(sgd) {
            weightsArray(w1_index + d) -= eta * g1
            weightsArray(w2_index + d) -= eta * g2
          } else {
              val wg1: Double = weightsArray(wg1_index + d) + g1 * g1
              val wg2: Double = weightsArray(wg2_index + d) + g2 * g2
              weightsArray(w1_index + d) -= eta / (math.sqrt(wg1)) * g1
              weightsArray(w2_index + d) -= eta / (math.sqrt(wg2)) * g2
              weightsArray(wg1_index + d) = wg1
              weightsArray(wg2_index + d) = wg2
            tt += 1
          }
        }
      }
    }
*/
    println("write:" + tt + " values")
    (BDV(weightsArray), tr_loss)
  }
}
/**
  * FFMParameter
  */
class FFMParameter extends Serializable {
  var eta: Double = 0.0
  var lambda: Double = 0.0
  var n_iters: Int = 0
  var k: Int = 0
  var normalization: Boolean = false
  var random: Boolean = false

  def defaultParameter: FFMParameter = {
    val parameter: FFMParameter = new FFMParameter
    parameter.eta = 0.1
    parameter.lambda = 0.0
    parameter.n_iters = 15
    parameter.k = 4
    parameter.normalization = true
    parameter.random = true
    return parameter
  }
}

/**
  * FFMNode
  */
class FFMNode extends Serializable {
  var v: Double = 0.0
  // field_num
  var f: Int = 0
  // feature_num
  var j: Int = 0
  // value
}