package com.github.thinkami.railroads.ui

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes

class RailroadColor {
    companion object {
        private val DisabledColor = JBColor.GRAY

        // Delayed initialization delays acquisition of TextAttributesKey until runtime
        private val rubyMethodColor by lazy {
            TextAttributesKey.find("RUBY_METHOD_NAME").defaultAttributes.foregroundColor
        }

        val DisabledItemAttr = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN,
            DisabledColor
        )
        // SimpleTextAttributes is also initialized lazily.
        val RubyMethodAttr by lazy {
            SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                rubyMethodColor
            )
        }
    }
}