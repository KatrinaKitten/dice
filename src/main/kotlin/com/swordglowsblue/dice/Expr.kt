package com.swordglowsblue.dice

import java.util.*

interface Expr : Equatable {
  fun eval(): Result
  val source get() = this
  override fun toString(): String
  fun parenthesize(): String = "($this)"
}

class Const(val value: Int) : Expr {
  override fun eval() = Result(this, value)
  override fun hashCode() = value
  override fun toString() = "$value"
  override fun parenthesize() = toString()
  override fun equals(other: Any?) = equalsImpl(other)
}

sealed class BinaryOp(val lhs: Expr, val rhs: Expr, val text: String, val fn: (Int, Int) -> Int) : Expr {
  class Add(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "+", Int::plus) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  class Sub(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "-", Int::minus) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  class Mul(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "*", Int::times) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  class Div(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "/", Int::div) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  class Mod(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "%", Int::rem) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  class Pow(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "^", Int::pow) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }

  override fun eval(): Result {
    val lhs = lhs.eval()
    val rhs = rhs.eval()
    return Result(this, fn(lhs.value, rhs.value), children = listOf(lhs, rhs))
  }

  override fun toString() = "$lhs $text $rhs"
  override fun hashCode() = Objects.hash(lhs, rhs, text, fn)
  override fun equals(other: Any?) = equalsImpl(other)
}

class Evaluated(
  override val source: Expr,
  val result: Result = source.eval()
) : Expr by source {
  override fun eval() = result
  override fun toString() = "$source=${result.value}"
  override fun hashCode() = Objects.hash(source, result)
  override fun equals(other: Any?) = equalsImpl(other)

  override fun parenthesize(): String = when(source) {
    is Const -> source.toString()
    is BinaryOp -> "(($source)=${result.value})"
    is Evaluated -> source.parenthesize()
    else -> "($this)"
  }
}

data class Result(
  val source: Expr,
  val value: Int,
  val rolls: List<Int> = emptyList(),
  val children: List<Result> = emptyList()
) {
  constructor(source: Expr, rolls: List<Int> = emptyList(), children: List<Result> = emptyList())
    : this(source, rolls.sum(), rolls, children)
  constructor(source: Expr, rolls: List<Int> = emptyList(), vararg children: Result)
    : this(source, rolls.sum(), rolls, children.toList())
  constructor(source: Expr, value: Int, rolls: List<Int> = emptyList(), vararg children: Result)
    : this(source, value, rolls, children.toList())

  val childDice: List<Result> get() = children
    .filter { it.source is Dice || it.childDice.isNotEmpty() }
    .flatMap { if(it.source is Dice) listOf(it) else it.childDice }

  override fun toString() =
    "$source = $value ${rolls.takeIf(List<*>::isNotEmpty) ?: ""}".trim()
  fun mkString(): String = (toString() +
    (childDice.takeIf(List<*>::isNotEmpty)
      ?.map(Result::mkString)
      ?.joinToString("\n")
      ?.indent(2)
      ?.let { "\n$it" }
      ?: "")).trim()
}
