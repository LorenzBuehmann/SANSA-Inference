package net.sansa_stack.inference.spark.forwardchaining

import scala.reflect.ClassTag

import org.apache.spark.rdd.RDD

import net.sansa_stack.inference.utils.Logging

/**
  * Creates a new RDD by performing bulk iterations using the given step function. The first
  * RDD the step function returns is the input for the next iteration, the second RDD is
  * the termination criterion. The iterations terminate when either the termination criterion
  * RDD contains no elements or when `maxIterations` iterations have been performed.
  *
  * @author Lorenz Buehmann
  */
trait FixpointIteration[T] extends Logging {
  val rdd: RDD[T]
  val f: RDD[T] => RDD[T]
}

object FixpointIteration extends Logging {

  /**
    * Creates a new RDD by performing bulk iterations using the given step function `f`. The first
    * RDD the step function returns is the input for the next iteration, the second RDD is
    * the termination criterion. The iterations terminate when either the termination criterion
    * RDD contains no elements or when `maxIterations` iterations have been performed.
    *
    **/
  def apply[T: ClassTag](maxIterations: Int = 10)(rdd: RDD[T], f: RDD[T] => RDD[T]): RDD[T] = {
    var newRDD = rdd
    newRDD.cache()
    var i = 1
    var oldCount = 0L
    var nextCount = if (newRDD.isEmpty()) 0L else 1L
    while (nextCount != oldCount) {
      log.info(s"iteration $i...")
      oldCount = nextCount
      info(s"i:$nextCount")
      newRDD = newRDD
        .union(f(newRDD))
        .distinct(2)
        .cache()
      nextCount = newRDD.count()
      info(s"i+1:$nextCount")
      i += 1
    }
    newRDD
  }
}
