package com.github.thinkami.railroads.providers

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.intellij.ide.TextCopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread

open class RouteCopyProvider(private val routes: Array<BaseRoute>?): TextCopyProvider() {
    override fun getTextLinesToCopy(): MutableCollection<String>? {
        if (routes == null) {
            return null
        }

        val copyLines = routes.map {
            getCopyValue(it)
        }

        return copyLines.toMutableList()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    open fun getCopyValue(route: BaseRoute): String {
        return ""
    }
}