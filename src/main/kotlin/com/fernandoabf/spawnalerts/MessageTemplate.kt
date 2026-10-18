package com.fernandoabf.spawnalerts

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

/**
 * Turns a config template into a chat component: `&` (or `§`) colour codes become styles, and
 * `{name}` placeholders become the given components, styled like the text around them. Unknown
 * placeholders stay as text.
 */
object MessageTemplate {
    private val PLACEHOLDER = Regex("""\{(\w+)}""")

    fun format(template: String, values: Map<String, Component>): MutableComponent {
        val root = Component.empty()
        val text = StringBuilder()
        var style = Style.EMPTY

        fun flush() {
            if (text.isNotEmpty()) {
                root.append(Component.literal(text.toString()).withStyle(style))
                text.clear()
            }
        }

        var i = 0
        while (i < template.length) {
            val char = template[i]
            if ((char == '&' || char == '§') && i + 1 < template.length) {
                val formatting = ChatFormatting.getByCode(template[i + 1].lowercaseChar())
                if (formatting != null) {
                    flush()
                    style = if (formatting == ChatFormatting.RESET) Style.EMPTY else style.applyLegacyFormat(formatting)
                    i += 2
                    continue
                }
            }
            if (char == '{') {
                val match = PLACEHOLDER.find(template, i)?.takeIf { it.range.first == i }
                val value = match?.let { values[it.groupValues[1]] }
                if (match != null && value != null) {
                    flush()
                    root.append(value.copy().withStyle(style))
                    i += match.value.length
                    continue
                }
            }
            text.append(char)
            i++
        }
        flush()
        return root
    }
}
