package com.swordglowsblue.dice

interface Result<T: DiceExpr> {
  val expr: T
  val value: Int
  val subRolls: List<RollResult>
}

data class EvalResult(
  override val expr: DiceExpr,
  override val value: Int,
  override val subRolls: List<RollResult> = emptyList()
) : Result<DiceExpr> {
  constructor(expr: DiceExpr, value: Int, vararg subRolls: RollResult) : this(expr, value, subRolls.toList())
  override fun toString() =
    "${expr.exprString()} = $value\n${subRolls.joinToString("\n").trimIndent().indent()}".trim()
}

data class RollResult(
  override val expr: Dice,
  val rolls: List<Int>,
  override val subRolls: List<RollResult>
) : Result<Dice> {
  override val value = rolls.sum()

  constructor(expr: Dice, rolls: List<Int>) : this(expr, rolls, emptyList())
  constructor(expr: Dice, vararg subRolls: RollResult) : this(expr, expr.roll(), subRolls.toList())
  constructor(expr: Dice, rolls: List<Int>, vararg subRolls: RollResult) : this(expr, rolls, subRolls.toList())

  fun toEvalResult() = EvalResult(expr, value, this)
  fun decapitate() = RollResult(expr, rolls)
  fun withSubRolls(vararg subRolls: RollResult) = RollResult(expr, rolls, this.subRolls + subRolls.toList())

  override fun toString() =
    "${expr.exprString()} = $value $rolls\n${subRolls.joinToString("\n").indent()}".trim()
}
