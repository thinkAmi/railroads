package com.github.thinkami.railroads.ui

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes

class RailroadColor {
    companion object {
        private val DisabledColor = JBColor.GRAY
        private val RubyMethodColor = TextAttributesKey.find("RUBY_METHOD_NAME").defaultAttributes.foregroundColor

        val DisabledItemAttr = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN,
            DisabledColor
        )
        val RubyMethodAttr = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN,
            RubyMethodColor
        )
    }
}