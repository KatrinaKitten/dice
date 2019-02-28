package com.swordglowsblue.dice

interface DiceExpr {
  fun eval(): EvalResult

  override fun hashCode(): Int
  override fun toString(): String
  override fun equals(other: Any?): Boolean
  fun exprString() = toString()
  fun parenthesize() = "($this)"
}

interface Dice : DiceExpr {
  val lhsExpr: DiceExpr
  val rhsExpr: DiceExpr
  val lhs: EvalResult
  val rhs: EvalResult
  val count: Int
  val sides: Int

  fun roll(): List<Int>
  fun rollResult() = RollResult(this, *(lhs.subRolls + rhs.subRolls).toTypedArray())
  override fun eval() = rollResult().toEvalResult()
  override fun toString(): String

  abstract class Impl(
    override val lhsExpr: DiceExpr,
    override val rhsExpr: DiceExpr
  ) : Dice {
    override val lhs: EvalResult by lazy { lhsExpr.eval() }
    override val rhs: EvalResult by lazy { rhsExpr.eval() }
    override val count: Int by lazy { lhs.value }
    override val sides: Int by lazy { rhs.value }

    override fun hashCode() = listOf(this::class, lhsExpr, rhsExpr).fold(0) {a,b -> a*31+b.hashCode()}
    override fun equals(other: Any?) = other is Dice && this.hashCode() == other.hashCode()
  }
}

data class Const(val value: Int) : DiceExpr {
  override fun eval() = EvalResult(this, value)
  override fun toString() = "$value"
  override fun parenthesize() = "$this"
}

sealed class BinaryOp(
  open val lhs: DiceExpr,
  open val rhs: DiceExpr,
  val text: String,
  val fn: (Int, Int) -> Int
) : DiceExpr {
  class Add(lhs: DiceExpr, rhs: DiceExpr) : BinaryOp(lhs, rhs, "+", Int::plus)
  class Sub(lhs: DiceExpr, rhs: DiceExpr) : BinaryOp(lhs, rhs, "-", Int::minus)
  class Mul(lhs: DiceExpr, rhs: DiceExpr) : BinaryOp(lhs, rhs, "*", Int::times)
  class Div(lhs: DiceExpr, rhs: DiceExpr) : BinaryOp(lhs, rhs, "/", Int::div)
  class Mod(lhs: DiceExpr, rhs: DiceExpr) : BinaryOp(lhs, rhs, "%", Int::rem)
  class Pow(lhs: DiceExpr, rhs: DiceExpr) : BinaryOp(lhs, rhs, "^", Int::pow)

  override fun toString() = "${lhs.exprString()} $text ${rhs.exprString()}"
  override fun eval(): EvalResult {
    val l = lhs.eval()
    val r = rhs.eval()
    return EvalResult(this, fn(l.value,r.value), l.subRolls + r.subRolls)
  }

  override fun hashCode() = listOf(this::class, lhs, rhs).fold(0) {a,b -> a*31+b.hashCode()}
  override fun equals(other: Any?) = other is BinaryOp && this.hashCode() == other.hashCode()
}

class BasicDice(lhs: DiceExpr, rhs: DiceExpr) : Dice.Impl(lhs, rhs) {
  constructor(lhs: Int, rhs: Int) : this(Const(lhs), Const(rhs))
  override fun toString() = "${count}d$sides"
  override fun exprString() = "${lhsExpr.parenthesize()}d${rhsExpr.parenthesize()}"
  override fun roll() = List(count) { (1..sides).random() }
}

class FateDice(lhs: DiceExpr) : Dice.Impl(lhs, Const(0)) {
  constructor(lhs: Int) : this(Const(lhs))
  override fun toString() ="${count}dF"
  override fun exprString() = "${lhsExpr.parenthesize()}dF"
  override fun roll() = List(count) { (-1..1).random() }
}

sealed class DiceFn(val of: Dice) : Dice by of {
  abstract override fun toString(): String
  abstract override fun rollResult(): RollResult

  // Override hashCode/equals to prevent DiceFn(dice) == dice
  override fun hashCode() = listOf(this::class, of).fold(0) {a,b -> a*31+b.hashCode()}
  override fun equals(other: Any?) = other is DiceFn && hashCode() == other.hashCode()

  class Explode(of: Dice) : DiceFn(of) {
    override fun toString() = "$of!"
    override fun rollResult() = rollResult(this, emptyList())

    private fun rollResult(of: Dice, acc: List<Int>): RollResult {
      val result = of.roll()
      if(of.sides <= 1 || of.sides !in result)
        return RollResult(this, acc + result, lhs.subRolls + rhs.subRolls)
      return rollResult(BasicDice(result.count{it==of.sides}, of.sides), acc + result)
    }
  }

  class Advantage(of: Dice) : DiceFn(of) {
    override fun toString() = "$of adv"
    override fun rollResult(): RollResult {
      val x = of.rollResult()
      val y = of.rollResult()
      return (if(x.value > y.value) x else y).decapitate().withSubRolls(x,y)
    }
  }

  class Disadvantage(of: Dice) : DiceFn(of) {
    override fun toString() = "$of dis"
    override fun rollResult(): RollResult {
      val x = of.rollResult()
      val y = of.rollResult()
      return (if(x.value < y.value) x else y).decapitate().withSubRolls(x,y)
    }
  }
}
