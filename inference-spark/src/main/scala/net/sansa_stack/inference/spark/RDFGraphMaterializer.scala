package net.sansa_stack.inference.spark

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import net.sansa_stack.inference.data.RDFTriple
import net.sansa_stack.inference.spark.data.{RDFGraphLoader, RDFGraphWriter}
import net.sansa_stack.inference.spark.forwardchaining.ForwardRuleReasonerRDFS
import net.sansa_stack.inference.utils.RuleUtils

/**
  * The class to compute the RDFS materialization of a given RDF graph.
  *
  * @author Lorenz Buehmann
  *
  */
object RDFGraphMaterializer {


  def main(args: Array[String]) {

    if (args.length < 2) {
      System.err.println("Usage: RDFGraphMaterializer <sourceFile> <targetFile>")
      System.exit(1)
    }

    val conf = new SparkConf()
    conf.registerKryoClasses(Array(classOf[RDFTriple]))

    // the SPARK config
    val session = SparkSession.builder
      .appName("SPARK Reasoning")
      .master("local[4]")
      .config("spark.eventLog.enabled", "true")
      .config("spark.hadoop.validateOutputSpecs", "false") //override output files
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config(conf)
      .getOrCreate()

    // load triples from disk
    val graph = RDFGraphLoader.loadFromFile(args(0), session.sparkContext, 4)

    // create reasoner
    val reasoner = new ForwardRuleReasonerRDFS(session.sparkContext)

    // compute inferred graph
    val inferredGraph = reasoner.apply(graph)
    print(inferredGraph.size())

    // write triples to disk
    RDFGraphWriter.writeToFile(inferredGraph, args(1))

    session.stop()
  }

}