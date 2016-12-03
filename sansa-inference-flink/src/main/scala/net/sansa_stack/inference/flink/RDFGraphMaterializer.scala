package net.sansa_stack.inference.flink

import java.io.File

import net.sansa_stack.inference.flink.data.{RDFGraphLoader, RDFGraphWriter}
import net.sansa_stack.inference.flink.forwardchaining.{ForwardRuleReasonerOWLHorst, ForwardRuleReasonerRDFS}
import net.sansa_stack.inference.rules.ReasoningProfile
import net.sansa_stack.inference.rules.ReasoningProfile._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala.ExecutionEnvironment

/**
  * The class to compute the RDFS materialization of a given RDF graph.
  *
  * @author Lorenz Buehmann
  *
  */
object RDFGraphMaterializer {

  def main(args: Array[String]) {
    parser.parse(args, Config()) match {
      case Some(config) =>
        run(args, config.in, config.out, config.profile, config.writeToSingleFile, config.sortedOutput)
      case None =>
        println(parser.usage)
    }
  }

  def run(args: Array[String], input: Seq[File], output: File, profile: ReasoningProfile, writeToSingleFile: Boolean, sortedOutput: Boolean): Unit = {
    // get params
    val params: ParameterTool = ParameterTool.fromArgs(args)

    // set up the execution environment
    val env = ExecutionEnvironment.getExecutionEnvironment

    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    // load triples from disk
    val graph = RDFGraphLoader.loadFromDisk(input(0), env)
    print(graph.size())

    // create reasoner
    val reasoner = profile match {
      case RDFS => new ForwardRuleReasonerRDFS(env)
      case OWL_HORST => new ForwardRuleReasonerOWLHorst(env)
    }

    // compute inferred graph
    val inferredGraph = reasoner.apply(graph)
    println(s"|G| = $inferredGraph.size()")

    // write triples to disk
    RDFGraphWriter.writeToDisk(inferredGraph, output)

    env.execute(s"RDF ${profile} Reasoning")

  }

  // the config object
  case class Config(
                     in: Seq[File] = Seq(),
                     out: File = new File("."),
                     profile: ReasoningProfile = ReasoningProfile.RDFS,
                     writeToSingleFile: Boolean = false,
                     sortedOutput: Boolean = false)

  // read ReasoningProfile enum
  implicit val profilesRead: scopt.Read[ReasoningProfile.Value] =
    scopt.Read.reads(ReasoningProfile forName _.toLowerCase())

  // the CLI parser
  val parser = new scopt.OptionParser[Config]("RDFGraphMaterializer") {
    head("RDFGraphMaterializer", "0.1.0")

    opt[Seq[File]]('i', "input").required().valueName("<path1>,<path2>,...").
      action((x, c) => c.copy(in = x)).
      text("path to file or directory that contains the input files (in N-Triple format)")

    opt[File]('o', "out").required().valueName("<directory>").
      action((x, c) => c.copy(out = x)).
      text("the output directory")

    opt[Unit]("single-file").optional().action( (_, c) =>
      c.copy(writeToSingleFile = true)).text("write the output to a single file in the output directory")

    opt[Unit]("sorted").optional().action( (_, c) =>
      c.copy(sortedOutput = true)).text("sorted output of the triples (per file)")

    opt[ReasoningProfile]('p', "profile").required().valueName("{rdfs | owl-horst | owl-el | owl-rl}").
      action((x, c) => c.copy(profile = x)).
      text("the reasoning profile")

    help("help").text("prints this usage text")
  }

}