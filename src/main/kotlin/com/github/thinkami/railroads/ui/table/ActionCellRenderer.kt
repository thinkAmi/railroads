package com.github.thinkami.railroads.ui.table

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.models.routes.RedirectRoute
import com.github.thinkami.railroads.ui.RailroadColor
import com.intellij.ui.ColoredTableCellRenderer
import javax.swing.JTable

class ActionCellRenderer: ColoredTableCellRenderer() {
    override fun customizeCellRenderer(
        table: JTable,
        value: Any?,
        selected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ) {
        if (value == null) {
            return
        }

        val route = value as BaseRoute
        renderRouteAction(route)

    }

    private fun renderRouteAction(route: BaseRoute) {
        icon = route.getActionIcon()
        val actionTitle = route.getActionTitle()

        if (route is RedirectRoute) {
            append(actionTitle)
            return
        }

        if (route.canNavigate()) {
            val controllerAndMethod = actionTitle.split("#")

            append("${controllerAndMethod[0]}#")

            if (route.methodExists()) {
                append(controllerAndMethod[1], RailroadColor.RubyMethodAttr)
            } else {
                append(controllerAndMethod[1], RailroadColor.DisabledItemAttr)
            }
            return
        }

        append(actionTitle, RailroadColor.DisabledItemAttr)
    }

}