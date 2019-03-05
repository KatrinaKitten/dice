# Expression Syntax

Most basic [dice notation](https://en.wikipedia.org/wiki/Dice_notation) should work outright. The table below outlines 
in detail all syntax supported by this library. 

- `{Expr}` represents any expression from the below table (see [Operator Precedence](#operator-precedence) for details).
- `{Dice}` represents any expression considered to be dice.
- All expressions are case-insensitive.
- All numbers are 32-bit integers (`Int`) - floats (`Float`) or integers larger than 32 bits are not supported 
in expressions or results.

| Syntax               | Description                                           | Is dice?
| :---              | :---                                          | :---
| Any positive integer | A constant number (1, 7, 9632, ...)                   | No
| `({Expr})`           | Grouping                                              | No
| `{Expr} + {Expr}`    | Addition                                              | No
| `{Expr} - {Expr}`    | Subtraction                                           | No
| `{Expr} * {Expr}`    | Multiplication                                        | No
| `{Expr} / {Expr}`    | Integer division (rounded down)                       | No
| `{Expr} % {Expr}`    | Modulus (remainder)                                   | No
| `{Expr} ^ {Expr}`    | Exponentiation                                        | No
| `{Expr}d{Expr}`      | Simple dice (left side is count, right side is sides) | Yes
| `{Expr}dF`           | Fate dice (sides range from -1 to 1)                  | Yes
| `{Dice}!`            | Exploding dice (max rolls are recursively rerolled)   | Yes
| `{Dice} adv`         | Advantage (higher of two rolls)                       | Yes
| `{Dice} dis`         | Disadvantage (lower of two rolls)                     | Yes

### Planned Syntax
These syntax elements are planned for the future, but are not currently available. They may require different parsing functions
or other similar solutions, depending on their complexity.

| Syntax                 | Description          | Is dice? | Supported alternative
| :---                | :---              | :---    | :---
| `-{Expr}`              | Unary negation       | No       | `0-{Expr}`
| `d{Expr}`              | Single-die shorthand | Yes      | `1d{Expr}`
| `$` followed by a name | Variable reference   | Maybe    |
| `{Expr}, {Expr}`       | Multiple expressions | No       | Multiple calls to `DiceParser.parse`

### Operator Precedence
The following table outlines operator precedence from highest to lowest. 

- Higher precedence binds more tightly (`1 + 2 * 3` is parsed as `1 + (2 * 3)`, not `(1 + 2) * 3`). 
- All operators are left-associative (`1 * 2 * 3` is parsed as `(1 * 2) * 3`, not `1 * (2 * 3)`).

| Operators         | Group name
| :---            | :---
| `()`              | Explicit grouping
| `d`, `dF`         | Core dice expressions
| `!`, `adv`, `dis` | Dice modifiers
| `^`               | Exponentiation
| `*`, `/`, `%`     | Multiplication and division
| `+`, `-`          | Addition and subtraction  
