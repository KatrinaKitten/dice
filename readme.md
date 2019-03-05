# Dice

A powerful and expressive dice expression parser.

## Installation

You can install Dice with JitPack (Gradle example shown below), check out the Releases page, or download the source code and build it yourself.
```gradle
allprojects {
  repositories {
    // ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  implementation 'com.github.swordglowsblue:dice:master-SNAPSHOT'
}
```

## API Docs
See [here](https://docs.swordglowsblue.com/dice).

## Expression Syntax
Most basic [dice notation](https://en.wikipedia.org/wiki/Dice_notation) should work outright.
See the wiki page for [Expression Syntax](https://docs.swordglowsblue.com/dice/expression-syntax) for details.

## Examples

### Parsing a dice expression
```kotlin
import com.swordglowsblue.dice.*

val expr: DiceExpr = DiceParser.parse("4d6")
  // => BasicDice(Const(4), Const(6))
```

### Evaluating a parsed expression
```kotlin
val result: EvalResult = expr.eval()
result.expr     //: DiceExpr - The expression
result.value    //: Int - The result of the expression
result.subRolls //: List<RollResult> - Dice rolls made during evaluation
```

### Working with `RollResult`
```kotlin
val rollResult: RollResult = expr.subRolls[0]
result.expr     //: Dice - The dice that were rolled
result.value    //: Int - The result of the roll
result.rolls    //: List<Int> - Individual die results
result.subRolls //: List<RollResult> - Other rolls made as part of this roll
```
