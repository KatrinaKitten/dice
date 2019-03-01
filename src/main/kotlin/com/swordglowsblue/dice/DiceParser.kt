package com.swordglowsblue.dice

object DiceParser {
  private sealed class Token(val text: String) {
    interface DieOp
    interface BoundOp { val opr: Operator }
    interface Operator { val text: String }

    class Number(text: String) : Token(text)
    object OpenParen  : Token("(")
    object CloseParen : Token(")")
    object Add : Token("+"), Operator
    object Sub : Token("-"), Operator
    object Mul : Token("*"), Operator
    object Div : Token("/"), Operator
    object Mod : Token("%"), Operator
    object Pow : Token("^"), Operator

    object Die : Token("d"), Operator, DieOp
    object DieFate : Token("dF"), Operator, DieOp
    object DieExpl : Token("!"), Operator, DieOp
    object Adv : Token("adv"), Operator, DieOp
    object Dis : Token("dis"), Operator, DieOp

    class UnaryOp  (val opn: Token, override val opr: Operator) : Token("($opr $opn)"), BoundOp
    class BinaryOp (val lhs: Token, override val opr: Operator, val rhs: Token) : Token("($lhs $opr $rhs)"), BoundOp

    override fun toString() = text

    fun requireBoundDieOp(by: Operator) {
      if(this !is Token.BoundOp || this.opr !is Token.DieOp)
        throw DiceParserException("The ${by.text} operator can only be used following a die roll")
    }
  }

  private tailrec fun lex(from: String, tokens: List<Token> = emptyList()): List<Token> = when(val c = from.head) {
    null -> tokens
    in Regex("\\s") -> lex(from.trim(), tokens)
    '(' -> lex(from.tail, tokens + Token.OpenParen)
    ')' -> lex(from.tail, tokens + Token.CloseParen)
    '+' -> lex(from.tail, tokens + Token.Add)
    '-' -> lex(from.tail, tokens + Token.Sub)
    '*' -> lex(from.tail, tokens + Token.Mul)
    '/' -> lex(from.tail, tokens + Token.Div)
    '%' -> lex(from.tail, tokens + Token.Mod)
    '^' -> lex(from.tail, tokens + Token.Pow)
    '!' -> lex(from.tail, tokens + Token.DieExpl)

    in Regex("[a-zA-Z]") -> when(val str = from.takeWhile { it.isLetter() }.toLowerCase()) {
      "d"   -> lex(from.tail, tokens + Token.Die)
      "df"  -> lex(from.drop(2), tokens + Token.DieFate)
      "adv" -> lex(from.drop(3), tokens + Token.Adv)
      "dis" -> lex(from.drop(3), tokens + Token.Dis)
      else ->
        throw DiceParserException("Encountered unexpected token $str")
    }

    in Regex("\\d") -> {
      val num = from.takeWhile { it.isDigit() }
      lex(from.drop(num.length), tokens + Token.Number(num))
    }

    else ->
      throw DiceParserException("Encountered unexpected character $c")
  }

  fun parse(from: String) = convert(parse(lex(from)))
  private fun parse(from: List<Token>): Token {
    if(from.isEmpty()) throw DiceParserException("Cannot parse an empty expression")

    var current = 0
    var tokens = from
    val curr = { tokens[current] }
    val prev = { tokens.getOrNull(current-1) ?: throw DiceParserException("Expected token before ${curr()}, was missing") }
    val next = { tokens.getOrNull(current+1) ?: throw DiceParserException("Expected token after ${curr()}, was missing") }
    val chomp = { cond: () -> Boolean, fn: () -> Unit ->
      while(current < tokens.size) if(cond()) fn() else current++; current = 0 }

    // Consumes a pair of parentheses and replaces them and their contents with a Token.Parens
    val consumeParens: () -> Unit = {
      val (body, _) = run loop@{ tokens.drop(current+1).fold(emptyList<Token>() to 0) { (body, nest), token ->
        body+token to nest+when(token) {
          is Token.OpenParen -> 1
          is Token.CloseParen -> if(nest > 0) -1 else return@loop body to nest
          else -> 0
        }
      }}

      tokens = tokens.take(current) + parse(body) + tokens.drop(current + body.size + 2)
      current++
    }

    // Consumes a binary operator and replaces it and its operands with a Token.BinaryOp
    val collapseBinop = {
      val head = tokens.take(maxOf(current - 1, 0))
      val tail = tokens.drop(current + 2)
      tokens = head + Token.BinaryOp(prev(), curr() as Token.Operator, next()) + tail
    }

    // Consumes a postfix operator and replaces it and its operands with a Token.UnaryOp
    val collapsePostop = {
      val head = tokens.take(maxOf(current - 1, 0))
      val tail = tokens.drop(current + 1)
      tokens = head + Token.UnaryOp(prev(), curr() as Token.Operator) + tail
    }

    chomp({ curr() is Token.OpenParen                                             }, consumeParens  )
    chomp({ curr() is Token.Die                                                   }, collapseBinop  )
    chomp({ curr() is Token.DieFate                                               }, collapsePostop )
    chomp({ curr() is Token.DieExpl || curr() is Token.Adv || curr() is Token.Dis }, collapsePostop )
    chomp({ curr() is Token.Pow                                                   }, collapseBinop  )
    chomp({ curr() is Token.Mul || curr() is Token.Div || curr() is Token.Mod     }, collapseBinop  )
    chomp({ curr() is Token.Add || curr() is Token.Sub                            }, collapseBinop  )
    if(tokens.size > 1) throw DiceParserException("Leftover tokens were present after parsing")
    return tokens[0]
  }

  private fun convert(ast: Token): Expr = when(ast) {
    is Token.BinaryOp -> when(ast.opr) {
      is Token.Die -> BasicDice(convert(ast.lhs), convert(ast.rhs))
      else -> when(ast.opr.text) {
        "+" -> BinaryOp.Add(convert(ast.lhs), convert(ast.rhs))
        "-" -> BinaryOp.Sub(convert(ast.lhs), convert(ast.rhs))
        "*" -> BinaryOp.Mul(convert(ast.lhs), convert(ast.rhs))
        "/" -> BinaryOp.Div(convert(ast.lhs), convert(ast.rhs))
        "%" -> BinaryOp.Mod(convert(ast.lhs), convert(ast.rhs))
        "^" -> BinaryOp.Pow(convert(ast.lhs), convert(ast.rhs))
        else -> throw NotImplementedError(ast.toString())
      }
    }

    is Token.UnaryOp -> when(ast.opr) {
      is Token.DieFate -> FateDice(convert(ast.opn))
      is Token.DieExpl -> {
        ast.opn.requireBoundDieOp(ast.opr)
        DiceFn.Explode(convert(ast.opn) as Dice)
      }

      is Token.Adv -> {
        ast.opn.requireBoundDieOp(ast.opr)
        DiceFn.Advantage(convert(ast.opn) as Dice)
      }

      is Token.Dis -> {
        ast.opn.requireBoundDieOp(ast.opr)
        DiceFn.Disadvantage(convert(ast.opn) as Dice)
      }

      else -> throw NotImplementedError(ast.toString())
    }

    is Token.Number -> Const(ast.text.toInt())
    else -> throw DiceParserException("Cannot convert token $ast to DiceExpr")
  }

  class DiceParserException(message: String) : Exception(message)
}
