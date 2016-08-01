package org.sansa.inference.rules.plan

import org.apache.jena.graph.{Node, Triple}
import org.apache.jena.reasoner.TriplePattern
import org.sansa.inference.utils.TripleUtils

import scala.collection.mutable

/**
  * An execution plan to process a single rule.
  *
  * @author Lorenz Buehmann
  */
case class Plan(triplePatterns: Set[Triple], target: Triple, joins: mutable.Set[Join]) {

  val aliases = new mutable.HashMap[Triple, String]()
  var idx = 0


  def generateJoins() = {

  }

  def addTriplePattern(tp: TriplePattern) = {

  }

  def toSQL = {
    var sql = "SELECT "

    sql += projectionPart() + "\n"

    sql += fromPart() + "\n"

    sql += wherePart() + "\n"

    sql
  }

  def projectionPart(): String = {
    var sql = ""

    val requiredVars = TripleUtils.nodes(target)

    val expressions = mutable.ArrayBuffer[String]()

//    expressions += (if(target.getSubject.isVariable) expressionFor(target.getSubject, target) else target.getSubject.toString)
//    expressions += (if(target.getPredicate.isVariable) expressionFor(target.getPredicate, target) else target.getPredicate.toString)
//    expressions += (if(target.getObject.isVariable) expressionFor(target.getObject, target) else target.getObject.toString)

    requiredVars.foreach{ v =>
      if(v.isVariable) {
        var done = false

        for(tp <- triplePatterns; if !done) {
          val expr = expressionFor(v, tp)

          if(expr != "NULL") {
            expressions += expr
            done = true
          }
        }
      } else {
        expressions += "'" + v.toString + "'"
      }
    }

    sql += expressions.mkString(", ")

    sql
  }

  def fromPart(): String = {
    var sql = " FROM "

    // convert to list of pairs (1,2), (2,3), (3,4)
    val list = triplePatterns.toList.sliding(2).collect { case List(a, b) => (a, b) }.toList

    val pair = list(0)
    val tp1 = pair._1
    val tp2 = pair._2
    sql += fromPart(tp1) + " INNER JOIN " + fromPart(tp2) + " ON " + joinExpressionFor(joinsFor(tp1, tp2)) + " "

    for (i <- 1 until list.length) {
      val pair = list(i)
      val tp1 = pair._1
      val tp2 = pair._2
      sql += " INNER JOIN " + fromPart(tp2) + " ON " + joinExpressionFor(joinsFor(tp1, tp2)) + " "
    }


    //    sql += triplePatterns.map(tp => fromPart(tp)).mkString(" INNER JOIN ")
    //    sql += " ON " + joins.map(join => joinExpressionFor(join)).mkString(" AND ")
    sql
  }

  def joinsFor(tp1: Triple, tp2: Triple): Join = {
    joins.filter(join => (join.tp1 == tp1 || join.tp2 == tp1) && (join.tp1 == tp2 || join.tp2 == tp2)).head
  }

  def wherePart(): String = {
    var sql = " WHERE "
    val expressions = mutable.ArrayBuffer[String]()

    expressions ++= triplePatterns.flatMap(tp => whereParts(tp))
//    expressions ++= joins.map(join => joinExpressionFor(join))

    sql += expressions.mkString(" AND ")

    sql
  }

  def toSQL(tp: Triple) = {
    var sql = "SELECT "

    sql += projectionPart(tp)

    sql += " FROM " + fromPart(tp)

    sql += " WHERE " + whereParts(tp).mkString(" AND ")

    sql
  }

  def projectionPart(tp: Triple) = {
    subjectColumn() + ", " + predicateColumn() + ", " + objectColumn()
  }

  def projectionPart(tp: Triple, selectedVars: List[Node]) = {

  }

  def uniqueAliasFor(tp: Triple): String = {
    aliases.get(tp) match {
      case Some(alias) => alias
      case _ =>
        val alias = "rel" + idx
        aliases += tp -> alias
        idx += 1
        alias
    }
  }

  def joinExpressionFor(tp1: Triple, tp2: Triple, joinVar: Node) = {
    expressionFor(joinVar, tp1) + "=" + expressionFor(joinVar, tp2)
  }

  def joinExpressionFor(join: Join) = {
    expressionFor(join.joinVar, join.tp1) + "=" + expressionFor(join.joinVar, join.tp2)
  }

  def fromPart(tp: Triple) = {
    tableName(tp)
  }

  def expressionFor(variable: Node, tp: Triple): String = {
    val ret =
      if (tp.subjectMatches(variable)) {
        subjectColumnName(tp)
      } else if (tp.predicateMatches(variable)) {
        predicateColumnName(tp)
      } else if (tp.objectMatches(variable)) {
        objectColumnName(tp)
      } else {
        "NULL"
      }
    ret
  }

  def isVarWithName(node: Node) = {

  }

  def whereParts(tp: Triple) = {
    val res = mutable.Set[String]()

    if(!tp.getSubject.isVariable) {
      res += subjectColumnName(tp) + "='" + tp.getSubject + "'"
    }

    if(!tp.getPredicate.isVariable) {
      res += predicateColumnName(tp) + "='" + tp.getPredicate + "'"
    }

    if(!tp.getObject.isVariable) {
      res += objectColumnName(tp) + "='" + tp.getObject + "'"
    }
    res
  }

  def subjectColumnName(tp: Triple) = {
    uniqueAliasFor(tp) + "." + subjectColumn()
  }

  def predicateColumnName(tp: Triple) = {
    uniqueAliasFor(tp) + "." + predicateColumn()
  }

  def objectColumnName(tp: Triple) = {
    uniqueAliasFor(tp) + "." + objectColumn()
  }

  def tableName(tp: Triple) = {
    table() + " " + uniqueAliasFor(tp)
  }

  def table() = {
    "TRIPLES"
  }

  def subjectColumn() = {
    "subject"
  }

  def predicateColumn() = {
    "predicate"
  }

  def objectColumn() = {
    "object"
  }


}