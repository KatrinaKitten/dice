package com.swordglowsblue.dice

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.matchers.numerics.shouldBeInRange
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class DiceTest : StringSpec({
  "Expr.eval should produce the correct value for non-dice" {
    Const(1).eval().value shouldBe 1
    BinaryOp.Add(3, 3).eval().value shouldBe 6
    BinaryOp.Sub(3, 3).eval().value shouldBe 0
    BinaryOp.Mul(3, 3).eval().value shouldBe 9
    BinaryOp.Div(3, 3).eval().value shouldBe 1
    BinaryOp.Mod(3, 3).eval().value shouldBe 0
    BinaryOp.Pow(3, 3).eval().value shouldBe 27
  }

  repeat(1000, "Expr.eval should produce values in the correct range for dice") {
    BasicDice(4, 6).eval().value shouldBeInRange 4..24
    FateDice(4).eval().value shouldBeInRange -4..4
    DiceFn.Explode(4, 6).eval().value shouldBeInRange 4..Int.MAX_VALUE
    DiceFn.Advantage(4, 6).eval().value shouldBeInRange 4..24
    DiceFn.Disadvantage(4, 6).eval().value shouldBeInRange 4..24

    BinaryOp.Add(BasicDice(4, 6), 3).eval().value shouldBeInRange 7..27
    BinaryOp.Sub(BasicDice(4, 6), 3).eval().value shouldBeInRange 1..21
    BinaryOp.Mul(BasicDice(4, 6), 3).eval().value shouldBeInRange 12..72
    BinaryOp.Div(BasicDice(4, 6), 3).eval().value shouldBeInRange 1..8
    BinaryOp.Mod(BasicDice(4, 6), 3).eval().value shouldBeInRange 0..2
    BinaryOp.Pow(BasicDice(4, 6), 3).eval().value shouldBeInRange 64..13824
  }

  repeat(1000, "Dice.eval should produce rolls in the correct range") {
    BasicDice(4, 6).eval().rolls shouldHaveBounds 1..6
    FateDice(4).eval().rolls shouldHaveBounds -1..1
    DiceFn.Explode(4, 6).eval().rolls shouldHaveBounds 1..6
    DiceFn.Advantage(4, 6).eval().rolls shouldHaveBounds 1..6
    DiceFn.Disadvantage(4, 6).eval().rolls shouldHaveBounds 1..6
  }

  repeat(1000, "Dice.eval should produce subrolls in the correct range") {
    BasicDice(BasicDice(2, 4), BasicDice(2, 20)).eval().children.apply {
      this shouldHaveSize 2
      this[0].value shouldBeInRange 2..8
      this[1].value shouldBeInRange 2..40
      this[0].rolls shouldHaveBounds 1..4
      this[1].rolls shouldHaveBounds 1..20
    }

    FateDice(BasicDice(2, 4)).eval().children.apply {
      this shouldHaveSize 1
      this[0].value shouldBeInRange 2..8
      this[0].rolls shouldHaveBounds 1..4
    }

    DiceFn.Explode(BasicDice(BasicDice(2, 4), BasicDice(2, 20))).eval().children[0].children.apply {
      this shouldHaveSize 2
      this[0].value shouldBeInRange 2..8
      this[1].value shouldBeInRange 2..40
      this[0].rolls shouldHaveBounds 1..4
      this[1].rolls shouldHaveBounds 1..20
    }

    DiceFn.Advantage(2, 20).eval().children.apply {
      this shouldHaveSize 2
      this[0].value shouldBeInRange 2..40
      this[1].value shouldBeInRange 2..40
      this[0].rolls shouldHaveBounds 1..20
      this[1].rolls shouldHaveBounds 1..20
    }

    DiceFn.Disadvantage(2, 20).eval().children.apply {
      this shouldHaveSize 2
      this[0].value shouldBeInRange 2..40
      this[1].value shouldBeInRange 2..40
      this[0].rolls shouldHaveBounds 1..20
      this[1].rolls shouldHaveBounds 1..20
    }
  }

  repeat(1000, "DiceFn.Explode.eval should produce the correct number of values") {
    val result = DiceFn.Explode(10, 6).eval()
    result.rolls shouldHaveSize 10 + result.rolls.count { it == 6 }
  }

  repeat(1000, "DiceFn.Advantage.eval should produce the higher of two attempts") {
    val result = DiceFn.Advantage(1, 20).eval()
    val sr = result.children.flatMap{it.rolls}
    sr shouldHaveSize 2
    result.value shouldBe sr.max()!!
    result.value shouldBeGreaterThanOrEqual sr.min()!!
  }

  repeat(1000, "DiceFn.Disdvantage.eval should produce the lower of two attempts") {
    val result = DiceFn.Disadvantage(1, 20).eval()
    val sr = result.children.flatMap{it.rolls}
    sr shouldHaveSize 2
    result.value shouldBe sr.min()!!
    result.value shouldBeLessThanOrEqual sr.max()!!
  }

  repeat(5, "test") {
    val result = DiceFn.Disadvantage(BasicDice(4, DiceFn.Advantage(BasicDice(1, BinaryOp.Add(10, BasicDice(1, 4)))))).eval()
    println(result.mkString()+"\n")
    System.out.flush()
  }
})
