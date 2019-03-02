package com.swordglowsblue.dice

import java.util.*

/**
 * An evaluatable expression.
 *
 * @property source The original [Expr] for this expression (`this` for expressions that don't wrap another instance)
 */
interface Expr : Equatable {
  val source get() = this

  /** Evaluate this expression. */
  fun eval(): Result

  /** Stringify this expression and wrap it in parentheses if applicable. */
  fun parenthesize(): String = "($this)"
  override fun toString(): String
}

/**
 * A constant number.
 * @property value The value of this constant
 */
class Const(val value: Int) : Expr {
  override fun eval() = Result(this, value)
  override fun hashCode() = value
  override fun toString() = "$value"
  override fun parenthesize() = toString()
  override fun equals(other: Any?) = equalsImpl(other)
}

/**
 * A binary operator such as `+`, `*`, or `^`.
 * @property lhs The expression on the left-hand side of this operator
 * @property rhs The expression on the left-hand side of this operator
 * @property text The textual representation of this operator
 * @property fn The function used to execute this operator
 */
sealed class BinaryOp(val lhs: Expr, val rhs: Expr, val text: String, val fn: (Int, Int) -> Int) : Expr {
  /** The addition operator (`+`). */
  class Add(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "+", Int::plus) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  /** The subtraction operator (`-`). */
  class Sub(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "-", Int::minus) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  /** The multiplication operator (`*`). */
  class Mul(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "*", Int::times) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  /** The integer division operator (`/`). */
  class Div(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "/", Int::div) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  /** The modulus/remainder operator (`%`). */
  class Mod(lhs: Expr, rhs: Expr) : BinaryOp(lhs, rhs, "%", Int::rem) {
    constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
    constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
    constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs)) }
  /** The exponentiation operator (`^`). */
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

/**
 * A pre-evaluated expression. Used for storing results of sub-expressions.
 * @property result The result of evaluating the [source] expression.
 */
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

/**
 * The result of an expression.
 * @property source The expression that generated this result
 * @property value The final value of the expression
 * @property rolls A list of individual die rolls involved in this expression (empty for non-dice)
 * @property children A list of sub-expression [Result]s involved in this expression
 */
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

  /** All [children] generated from [Dice], or which have children for which [childDice] is not empty. */
  val childDice: List<Result> by lazy { children
    .filter { it.source is Dice || it.childDice.isNotEmpty() }
    .flatMap { if(it.source is Dice) listOf(it) else it.childDice }
  }

  /** Stringify this [Result], recursively including [children]. */
  fun mkString(): String = (toString() +
    (childDice.takeIf(List<*>::isNotEmpty)
      ?.map(Result::mkString)
      ?.joinToString("\n")
      ?.indent(2)
      ?.let { "\n$it" }
      ?: "")).trim()
  override fun toString() =
    "$source = $value ${rolls.takeIf(List<*>::isNotEmpty) ?: ""}".trim()
}
