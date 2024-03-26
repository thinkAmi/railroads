package com.github.thinkami.railroads.views

import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import com.intellij.ui.table.JBTable
import javax.swing.JButton
import javax.swing.JLabel

class MainView(toolWindow: ToolWindow) {
    val panelComponent = toolWindow.component.components.filterIsInstance<DialogPanel>().first()
    val scrollComponent = panelComponent.components.filterIsInstance<JBScrollPane>().first()
    val tableComponent: JBTable

    init {
        val viewportComponent = scrollComponent.components.filterIsInstance<JBViewport>().first {
            it.components.filterIsInstance<JBTable>().isNotEmpty()
        }
        tableComponent = viewportComponent.components.filterIsInstance<JBTable>().first()
    }


    fun renderRoutesWithUiThread(routes: List<BaseRoute>) {
        ApplicationManager.getApplication().invokeLater {
            showRouteLabels()
            switchHeaderMenu(true)
            switchRouteTable(true)

            val model = tableComponent.model as RoutesTableModel
            model.updateTableDataFromRoutes(routes)
        }
    }

    fun switchRouteTable(isVisible: Boolean) {
        scrollComponent.isVisible = isVisible
    }

    fun showRouteLabels() {
        panelComponent.components.filterIsInstance<JLabel>().filter {
            it.name != "routesCounter"
        }.map {
            it.isVisible = true
        }

        panelComponent.components.filterIsInstance<HyperlinkLabel>().map {
            it.isVisible = true
        }
    }

    fun switchHeaderMenu(isEnabled: Boolean) {
        panelComponent.components.filterIsInstance<JButton>().map {
            it.isEnabled = isEnabled
        }
    }

    fun switchHeaderMenuWithUiThread(isEnabled: Boolean) {
        ApplicationManager.getApplication().invokeLater {
            switchHeaderMenu(isEnabled)
        }
    }
}