package com.swordglowsblue.dice

import io.kotlintest.matchers.collections.shouldHaveLowerBound
import io.kotlintest.matchers.collections.shouldHaveUpperBound
import io.kotlintest.specs.AbstractStringSpec


fun AbstractStringSpec.repeat(n: Int, ctx: String, block: () -> Unit) =
  ctx { for(i in 0 until n) block() }

infix fun <T: Comparable<T>> Collection<T>.shouldHaveBounds(range: ClosedRange<T>) =
  this.shouldHaveBounds(range.start, range.endInclusive)
fun <T: Comparable<T>> Collection<T>.shouldHaveBounds(lower: T, upper: T) {
  this shouldHaveLowerBound lower
  this shouldHaveUpperBound upper
}
