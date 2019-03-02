package com.swordglowsblue.dice

import kotlin.math.pow

internal fun Int.pow(b: Int) = this.toDouble().pow(b).toInt()
internal fun String.indent(spaces:Int=2) =
  split("\n").map { " ".repeat(spaces)+it }.joinToString("\n")

internal val String.head get() = getOrNull(0)
internal val String.tail get() = drop(1)
internal val <T, C : Collection<T>> C.head get():T? = elementAtOrNull(0)
internal val <T, C : Collection<T>> C.tail get():List<T> = drop(1)
internal operator fun Regex.contains(text:Char):Boolean = contains(text.toString())
internal operator fun Regex.contains(text:CharSequence):Boolean = this.matches(text)

/** Forces implementation of equals/hashCode and exposes the equalsImpl method. */
interface Equatable {
  override fun hashCode(): Int
  override fun equals(other: Any?): Boolean
}

/**
 * Simple implementation of a generic equals. Based on reference, type, and hashCode.
 * Does not account for super.equals.
 */
inline fun <reified T : Equatable> T.equalsImpl(other: Any?) =
  other === this || (other is T && other.hashCode() == this.hashCode())
