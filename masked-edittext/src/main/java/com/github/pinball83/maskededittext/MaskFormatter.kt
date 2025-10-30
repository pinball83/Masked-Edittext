package com.github.pinball83.maskededittext

import kotlin.math.max

/**
 * Shared mask formatter used by both the classic view and Compose implementations.
 */
internal class MaskFormatter(
    private val maskPattern: String,
    private val placeholder: Char,
    private val formatPattern: String? = null
) {

    private val maskTemplate: String = if (maskPattern.isEmpty()) maskPattern else maskPattern.replace(placeholder, ' ')

    val validPositions: List<Int> = maskPattern.mapIndexedNotNull { index, char ->
        if (char == placeholder) index else null
    }

    private val slotCount: Int = validPositions.size

    fun mask(unmasked: CharSequence): String {
        if (maskPattern.isEmpty() || slotCount == 0) {
            return unmasked.toString()
        }

        val normalized = normalize(unmasked)
        val result = StringBuilder(maskTemplate)
        var readIndex = 0

        for (position in validPositions) {
            val value = normalized.getOrNull(readIndex)
            result[position] = value ?: ' '
            if (value != null) {
                readIndex++
            }
        }

        return result.toString()
    }

    fun unmask(maskedInput: CharSequence): String {
        if (maskPattern.isEmpty() || slotCount == 0) {
            return maskedInput.toString()
        }

        val result = StringBuilder()
        for (position in validPositions) {
            val value = maskedInput.getOrNull(position) ?: ' '
            result.append(if (value == ' ') ' ' else value)
        }
        return result.toString()
    }

    fun normalize(unmasked: CharSequence): String {
        if (slotCount == 0) {
            return unmasked.toString()
        }
        val filtered = unmasked.filterNot { it.isWhitespace() }
        return filtered.take(slotCount).toString()
    }

    fun isComplete(unmasked: CharSequence): Boolean {
        if (slotCount == 0) {
            return unmasked.isNotEmpty()
        }
        return normalize(unmasked).length == slotCount
    }

    fun formatOutput(unmasked: CharSequence): String {
        val pattern = formatPattern ?: return unmasked.toString()
        var formatted = pattern
        normalize(unmasked).forEachIndexed { index, char ->
            formatted = formatted.replace("[${index + 1}]", char.toString())
        }
        return formatted.replace(PLACEHOLDER_REGEX, "")
    }

    fun firstValidPosition(): Int? = validPositions.firstOrNull()

    fun lastValidPosition(): Int? = validPositions.lastOrNull()

    fun nearestValidPosition(position: Int): Int {
        if (validPositions.isEmpty()) return position
        val first = validPositions.first()
        val last = validPositions.last()
        if (position <= first) return first
        if (position >= last) return last

        var lower = first
        var upper = last
        for (candidate in validPositions) {
            if (candidate >= position) {
                upper = candidate
                break
            }
            lower = candidate
        }

        val distDown = position - lower
        val distUp = upper - position
        return if (distUp <= distDown) upper else lower
    }

    fun cursorPositionFor(unmaskedLength: Int): Int {
        if (validPositions.isEmpty()) return max(unmaskedLength, 0)
        if (unmaskedLength <= 0) return validPositions.first()
        return if (unmaskedLength >= validPositions.size) {
            validPositions.last()
        } else {
            validPositions[unmaskedLength]
        }
    }

    private fun CharSequence.getOrNull(index: Int): Char? = if (index in 0 until length) this[index] else null

    private companion object {
        val PLACEHOLDER_REGEX = "\\[\\d+]".toRegex()
    }
}
