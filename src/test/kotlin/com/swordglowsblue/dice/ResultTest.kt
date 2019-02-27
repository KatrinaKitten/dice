package com.swordglowsblue.dice

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class ResultTest : StringSpec({
  "EvalResult.toString should return properly formatted results" {
    Const(1).eval().toString() shouldBe "1 = 1"
    BinaryOp.Add(Const(3), Const(3)).eval().toString() shouldBe "3 + 3 = 6"
    BinaryOp.Sub(Const(3), Const(3)).eval().toString() shouldBe "3 - 3 = 0"
    BinaryOp.Mul(Const(3), Const(3)).eval().toString() shouldBe "3 * 3 = 9"
    BinaryOp.Div(Const(3), Const(3)).eval().toString() shouldBe "3 / 3 = 1"
    BinaryOp.Mod(Const(3), Const(3)).eval().toString() shouldBe "3 % 3 = 0"
    BinaryOp.Pow(Const(3), Const(3)).eval().toString() shouldBe "3 ^ 3 = 27"
  }

  repeat(1000, "EvalResult.toString should return properly formatted subrolls") {
    val result = BinaryOp.Add(BasicDice(Const(2), Const(3)), BasicDice(BasicDice(Const(2), Const(3)), Const(6))).eval()
    val (sub1, sub2) = result.subRolls
    val sub3 = sub2.subRolls[0]
    result.toString() shouldBe
      "2d3 + ${sub3.value}d6 = ${result.value}" +
      "\n  2d3 = ${sub1.value} ${sub1.rolls}" +
      "\n  ${sub3.value}d6 = ${sub2.value} ${sub2.rolls}" +
      "\n    2d3 = ${sub3.value} ${sub3.rolls}"
  }
})
