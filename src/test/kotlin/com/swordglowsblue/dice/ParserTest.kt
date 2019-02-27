package com.swordglowsblue.dice

import com.swordglowsblue.dice.DiceParser.DiceParserException

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class ParserTest : StringSpec({
  "DiceParser.parse should correctly parse simple expressions" {
    DiceParser.parse("1") shouldBe Const(1)
    DiceParser.parse("1+2") shouldBe BinaryOp.Add(Const(1), Const(2))
    DiceParser.parse("1-2") shouldBe BinaryOp.Sub(Const(1), Const(2))
    DiceParser.parse("1*2") shouldBe BinaryOp.Mul(Const(1), Const(2))
    DiceParser.parse("1/2") shouldBe BinaryOp.Div(Const(1), Const(2))
    DiceParser.parse("1%2") shouldBe BinaryOp.Mod(Const(1), Const(2))
    DiceParser.parse("1^2") shouldBe BinaryOp.Pow(Const(1), Const(2))
    DiceParser.parse("1d2") shouldBe BasicDice(1, 2)
    DiceParser.parse("1dF") shouldBe FateDice(1)
    DiceParser.parse("1d2!")   shouldBe DiceFn.Explode(BasicDice(1, 2))
    DiceParser.parse("1d2adv") shouldBe DiceFn.Advantage(BasicDice(1, 2))
    DiceParser.parse("1d2dis") shouldBe DiceFn.Disadvantage(BasicDice(1,2))
  }

  "DiceParser.parse should correctly parse complex expressions" {
    DiceParser.parse("1+2d6*3") shouldBe
      BinaryOp.Add(Const(1), BinaryOp.Mul(BasicDice(2, 6), Const(3)))
    DiceParser.parse("(1d4)d(1d20)!") shouldBe
      DiceFn.Explode(BasicDice(BasicDice(1, 4), BasicDice(1, 20)))
    DiceParser.parse("4d6!!!!") shouldBe
      DiceFn.Explode(DiceFn.Explode(DiceFn.Explode(DiceFn.Explode(BasicDice(4, 6)))))
  }

  "DiceParser.parse should throw on invalid syntax" {
    shouldThrow<DiceParserException> { DiceParser.parse("d4") } //TODO: Make this not throw!
      .message shouldBe "Expected token before d, was missing"
    shouldThrow<DiceParserException> { DiceParser.parse("4d") }
      .message shouldBe "Expected token after d, was missing"
    shouldThrow<DiceParserException> { DiceParser.parse("asdfa") }
      .message shouldBe "Encountered unexpected token asdfa"
    shouldThrow<DiceParserException> { DiceParser.parse("1!") }
      .message shouldBe "The ! operator can only be used following a die roll"
    shouldThrow<DiceParserException> { DiceParser.parse("1adv") }
      .message shouldBe "The adv operator can only be used following a die roll"
    shouldThrow<DiceParserException> { DiceParser.parse("1dis") }
      .message shouldBe "The dis operator can only be used following a die roll"
    shouldThrow<DiceParserException> { DiceParser.parse(";") }
      .message shouldBe "Encountered unexpected character ;"
    shouldThrow<DiceParserException> { DiceParser.parse("") }
      .message shouldBe "Cannot parse an empty expression"
    shouldThrow<DiceParserException> { DiceParser.parse("1 1") }
      .message shouldBe "Leftover tokens were present after parsing"
  }
})

