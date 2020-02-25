package com.intel.sparkColumnarPlugin.expression

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.aggregate._
import org.apache.spark.sql.catalyst.expressions.BindReferences.bindReferences
object ColumnarExpressionConverter extends Logging {

  var check_if_no_calculation = true

  def replaceWithColumnarExpression(expr: Expression, attributeSeq: Seq[Attribute] = null): Expression = expr match {
    case a: Alias =>
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      new ColumnarAlias(replaceWithColumnarExpression(a.child, attributeSeq), a.name)(
        a.exprId,
        a.qualifier,
        a.explicitMetadata)
    case a: AttributeReference =>
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      if (attributeSeq != null) {
        val bindReference = BindReferences.bindReference(expr, attributeSeq)
        val b = bindReference.asInstanceOf[BoundReference]
        new ColumnarBoundReference(b.ordinal, b.dataType, b.nullable)
      } else {
        a
      }
    case lit: Literal =>
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      new ColumnarLiteral(lit)
    case binArith: BinaryArithmetic =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarBinaryArithmetic.create(
        replaceWithColumnarExpression(binArith.left, attributeSeq),
        replaceWithColumnarExpression(binArith.right, attributeSeq),
        expr)
    case b: BoundReference =>
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      new ColumnarBoundReference(b.ordinal, b.dataType, b.nullable)
    case b: BinaryOperator =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarBinaryOperator.create(
        replaceWithColumnarExpression(b.left, attributeSeq),
        replaceWithColumnarExpression(b.right, attributeSeq),
        expr)
    case e: EndsWith =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarBinaryOperator.create(
        replaceWithColumnarExpression(e.left),
        replaceWithColumnarExpression(e.right),
        expr)
    case s: StartsWith =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarBinaryOperator.create(
        replaceWithColumnarExpression(s.left),
        replaceWithColumnarExpression(s.right),
        expr)
    case l: Like =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarBinaryOperator.create(
        replaceWithColumnarExpression(l.left),
        replaceWithColumnarExpression(l.right),
        expr)
    case i: In =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarInOperator.create(
        replaceWithColumnarExpression(i.value),
        i.list,
        expr)
    case ss: Substring =>
      check_if_no_calculation = false
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarTernaryOperator.create(
        replaceWithColumnarExpression(ss.str),
        replaceWithColumnarExpression(ss.pos),
        replaceWithColumnarExpression(ss.len),
        expr)
    case u: UnaryExpression =>
      if (!u.isInstanceOf[Cast]) {
        check_if_no_calculation = false
      }
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      ColumnarUnaryOperator.create(replaceWithColumnarExpression(u.child, attributeSeq), expr)
    case s: org.apache.spark.sql.execution.ScalarSubquery =>
      logInfo(s"${expr.getClass} ${expr} is supported, no_cal is $check_if_no_calculation.")
      new ColumnarScalarSubquery(s)
    case expr =>
      logWarning(s"${expr.getClass} ${expr} is not currently supported.")
      expr
  }

  def ifNoCalculation = check_if_no_calculation

  def reset(): Unit = {
    check_if_no_calculation = true
  }

}
