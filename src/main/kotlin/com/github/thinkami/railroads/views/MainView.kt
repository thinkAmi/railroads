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
    private val panelComponent = toolWindow.component.components.filterIsInstance<DialogPanel>().first()
    private val scrollComponent = panelComponent.components.filterIsInstance<JBScrollPane>().first()
    private val tableComponent: JBTable

    init {
        val viewportComponent = scrollComponent.components.filterIsInstance<JBViewport>().first {
            it.components.filterIsInstance<JBTable>().isNotEmpty()
        }
        tableComponent = viewportComponent.components.filterIsInstance<JBTable>().first()
    }

    fun renderDefaultWithUiThread() {
        ApplicationManager.getApplication().invokeLater {
            switchHeaderMenu(true)
            switchRouteTable(false)
            switchRouteLabels(false)
            switchRunRailsRoutesMessage(true)
            switchLoadingMessage(false)
            switchRaiseErrorMessage(false)
        }
    }

    fun renderLoadingWithUiThread() {
        ApplicationManager.getApplication().invokeLater {
            switchHeaderMenu(false)
            switchRouteTable(false)
            switchRouteLabels(false)
            switchRunRailsRoutesMessage(false)
            switchLoadingMessage(true)
            switchRaiseErrorMessage(false)
        }
    }

    fun renderErrorWithUiThread() {
        switchHeaderMenu(true)
        switchRouteTable(false)
        switchRouteLabels(false)
        switchRunRailsRoutesMessage(false)
        switchLoadingMessage(false)
        switchRaiseErrorMessage(true)
    }

    fun renderRoutesWithUiThread(routes: List<BaseRoute>) {
        ApplicationManager.getApplication().invokeLater {
            switchHeaderMenu(true)
            switchRouteTable(true)
            switchRouteLabels(true)
            switchRunRailsRoutesMessage(false)
            switchLoadingMessage(false)
            switchRaiseErrorMessage(false)

            val model = tableComponent.model as RoutesTableModel
            model.updateTableDataFromRoutes(routes)
        }
    }

    private fun switchRouteTable(isVisible: Boolean) {
        scrollComponent.isVisible = isVisible
    }

    private fun switchRunRailsRoutesMessage(isVisible: Boolean) {
        panelComponent.components.filterIsInstance<JLabel>().filter {
            it.name == "runRailsRoutesMessage"
        }.map {
            it.isVisible = isVisible
        }
    }

    private fun switchLoadingMessage(isVisible: Boolean) {
        panelComponent.components.filterIsInstance<JLabel>().filter {
            it.name == "loadingMessage"
        }.map {
            it.isVisible = isVisible
        }
    }

    private fun switchRaiseErrorMessage(isVisible: Boolean) {
        panelComponent.components.filterIsInstance<JLabel>().filter {
            it.name == "raiseErrorMessage"
        }.map {
            it.isVisible = isVisible
        }
    }

    private fun switchRouteLabels(isVisible: Boolean) {
        panelComponent.components.filterIsInstance<JLabel>().filter {
            it.name != "routesCounter" && it.name != "runRailsRoutesMessage"
        }.map {
            it.isVisible = isVisible
        }

        panelComponent.components.filterIsInstance<HyperlinkLabel>().map {
            it.isVisible = isVisible
        }
    }

    private fun switchHeaderMenu(isEnabled: Boolean) {
        panelComponent.components.filterIsInstance<JButton>().map {
            it.isEnabled = isEnabled
        }
    }
}