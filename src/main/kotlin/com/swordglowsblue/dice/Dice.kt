package com.swordglowsblue.dice

import java.util.*

/**
 * A rollable set of dice.
 * @property lhsExpr The expression on the left-hand side of the dice (count)
 * @property rhsExpr The expression on the right-hand side of the dice (sides)
 * @property source The original [Dice] for this expression (`this` for dice that don't wrap another instance)
 */
interface Dice : Expr {
  val lhsExpr: Expr
  val rhsExpr: Expr
  override val source get() = this

  /**
   * Create an identical instance of this die type, replacing the sides with new expressions if given.
   * @param lhs The expression to replace the left side with
   * @param rhs The expression to replace the right side with
   */
  fun clone(lhs: Expr? = null, rhs: Expr? = null): Dice
  fun clone(lhs: Int? = null, rhs: Int? = null) = clone(lhs?.let(::Const), rhs?.let(::Const))

  /** Convert this to a [Finalized] version, evaluating the expressions on both sides. */
  fun finalize() = object : Finalized.Impl(this) {}
  /** Convert this to a [Finalized] version, evaluating the expressions on both sides, and pass that instance to the given block. */
  fun <T> finalize(block: Finalized.() -> T): T = finalize().block()

  /**
   * A finalized [Dice] instance, with both sides evaluated.
   * @property lhs The result of evaluating [lhsExpr]
   * @property rhs The result of evaluating [rhsExpr]
   * @property count The number of dice in this set
   * @property sides The number of sides on each die in this set
   */
  sealed class Finalized : Dice {
    abstract val lhs: Result
    abstract val rhs: Result
    abstract val count: Int
    abstract val sides: Int

    /**
     * A finalized [Dice] instance, with both sides evaluated.
     * @param source The [Dice] to finalize
     */
    abstract class Impl internal constructor(override val source: Dice) : Finalized(), Dice by source {
      override val lhsExpr = Evaluated(source.lhsExpr)
      override val rhsExpr = Evaluated(source.rhsExpr)
      override val lhs = lhsExpr.result
      override val rhs = rhsExpr.result
      override val count = lhs.value
      override val sides = rhs.value

      override fun equals(other: Any?) = equalsImpl(other)
      override fun hashCode() = Objects.hash(lhs, rhs, count, sides)
      override fun toString() = source.clone(lhsExpr, rhsExpr).toString()
      override fun finalize() = this
      override fun <T> finalize(block: Finalized.() -> T): T = finalize().block()

      override fun clone(lhs: Expr?, rhs: Expr?) = object : Impl(source) {
        override val lhs = lhs?.eval() ?: this@Impl.lhs
        override val rhs = rhs?.eval() ?: this@Impl.rhs
      }
    }
  }
}

/**
 * Simple dice with expressions on both sides (NdS).
 * @param lhsExpr The expression to use as the left-hand side
 * @param rhsExpr The expression to use as the right-hand side
 */
class BasicDice(override val lhsExpr: Expr, override val rhsExpr: Expr) : Dice {
  constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
  constructor(lhs: Int, rhs: Expr) : this(Const(lhs), rhs)
  constructor(lhs: Expr, rhs: Int) : this(lhs, Const(rhs))

  override fun eval() = finalize {
    val rolls = List(count) { (1..sides).random() }
    Result(this, rolls, lhs, rhs)
  }

  override fun clone(lhs: Expr?, rhs: Expr?) = BasicDice(lhs ?: lhsExpr, rhs ?: rhsExpr)
  override fun toString() = "${lhsExpr.parenthesize()}d${rhsExpr.parenthesize()}"
  override fun hashCode() = Objects.hash(lhsExpr, rhsExpr)
  override fun equals(other: Any?) = equalsImpl(other)
}

/**
 * Fate dice (NdF, results from -1 to 1).
 * @param lhsExpr The expression to use as the left-hand side
 */
class FateDice(override val lhsExpr: Expr) : Dice {
  constructor(lhs: Int) : this(Const(lhs))

  override val rhsExpr = Const(0)
  override fun eval() = finalize {
    val rolls = List(count) { (-1..1).random() }
    Result(this, rolls, lhs)
  }

  override fun clone(lhs: Expr?, rhs: Expr?) = FateDice(lhs
    ?: lhsExpr)
  override fun toString() = "${lhsExpr.parenthesize()}dF"
  override fun hashCode() = Objects.hash(lhsExpr)
  override fun equals(other: Any?) = equalsImpl(other)
}

/**
 * A modifier wrapping another type of dice.
 * @param text The textual representation of this modifier
 * @param of The [Dice] to modify
 */
sealed class DiceFn(private val text: String, protected val of: Dice) : Dice by of {
  abstract override fun eval(): Result
  abstract override fun clone(lhs: Expr?, rhs: Expr?): DiceFn
  override fun toString() = "$of$text"
  override fun hashCode() = of.hashCode()
  override fun equals(other: Any?) = equalsImpl(other)

  override fun finalize() = object : Dice.Finalized.Impl(this) {}
  override fun <T> finalize(block: Dice.Finalized.() -> T): T = finalize().block()

  /** Exploding dice (NdS!, recursively reroll max rolls). */
  class Explode(of: Dice) : DiceFn("!", of) {
    constructor(lhs: Int, rhs: Int) : this(BasicDice(lhs, rhs))
    constructor(lhs: Expr, rhs: Expr) : this(BasicDice(lhs, rhs))

    override fun clone(lhs: Expr?, rhs: Expr?) = Explode(of.clone(lhs, rhs))
    override fun eval() = finalize {
      var res = listOf(of.clone(lhsExpr, rhsExpr).eval())
      var new = res[0].rolls
      while(sides > 1 && sides in new) {
        res += of.clone(new.count {it==sides}, sides).eval()
        new = res.last().rolls
      }

      Result(this, res.flatMap { it.rolls }, res)
    }
  }

  /** Advantage (NdSadv, higher of two rolls) */
  class Advantage(of: Dice) : DiceFn("adv", of) {
    constructor(lhs: Int, rhs: Int) : this(BasicDice(lhs, rhs))
    constructor(lhs: Expr, rhs: Expr) : this(BasicDice(lhs, rhs))

    override fun clone(lhs: Expr?, rhs: Expr?) = Advantage(of.clone(lhs, rhs))
    override fun finalize() = object : Dice.Finalized.Impl(this) {}

    override fun eval(): Result {
      val x = of.eval()
      val y = of.eval()
      val z = if(x.value > y.value) x else y
      return Result(Advantage(z.source as Dice), z.rolls, x, y)
    }
  }

  /** Disadvantage (NdSdis, lower of two rolls) */
  class Disadvantage(of: Dice) : DiceFn("dis", of) {
    constructor(lhs: Int, rhs: Int) : this(BasicDice(lhs, rhs))
    constructor(lhs: Expr, rhs: Expr) : this(BasicDice(lhs, rhs))

    override fun clone(lhs: Expr?, rhs: Expr?) = Disadvantage(of.clone(lhs, rhs))
    override fun finalize() = object : Dice.Finalized.Impl(this) {}

    override fun eval(): Result {
      val x = of.eval()
      val y = of.eval()
      val z = if(x.value < y.value) x else y
      return Result(Disadvantage(z.source as Dice), z.rolls, x, y)
    }
  }
}
