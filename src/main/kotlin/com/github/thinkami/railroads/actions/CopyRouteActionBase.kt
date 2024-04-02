package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.providers.RouteCopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

open class CopyRouteActionBase: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext

        val routes = PlatformDataKeys.SELECTED_ITEMS.getData(dataContext) as Array<BaseRoute>

        val provider = object: RouteCopyProvider(routes) {
            override fun getCopyValue(route: BaseRoute): String {
                return getRouteValue(route)
            }
        }

        provider.performCopy(dataContext)
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val dataContext = e.dataContext

        val selectedRoute = PlatformDataKeys.SELECTED_ITEM.getData(dataContext)

        presentation.isEnabled = selectedRoute != null
        presentation.isVisible = true
    }

    open fun getRouteValue(route: BaseRoute): String {
        return ""
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}