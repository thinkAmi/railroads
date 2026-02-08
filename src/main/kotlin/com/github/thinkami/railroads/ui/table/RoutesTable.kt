package com.github.thinkami.railroads.ui.table

import com.github.thinkami.railroads.helper.ProjectReadyScheduler
import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.intellij.openapi.actionSystem.*
import com.intellij.ui.PopupHandler
import com.intellij.ui.table.JBTable
import com.intellij.util.ArrayUtil
import java.awt.Component
import com.intellij.ui.DoubleClickListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * (Railways)
 * We should add some behavior to default JTable to be able copy custom data.
 *
 * Our component (JTable) should implement DataProvider interface. In this
 * case when action is invoked in context of this component, we will be able to
 * pass our own data to the action handler.
 */
class RoutesTable: JBTable(), DataProvider {

    /**
     * (Railways)
     * Constructs a default <code>JTable</code> that is initialized with a default
     * data model, a default column model, and a default selection
     * model.
     */
    init {
        addMouseListener(RailroadsPopupHandler())

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                val route = getSelectedRoute() ?: return false

                val project = route.module.project
                if (!ProjectReadyScheduler.isReady(project)) {
                    // Ensure navigation happens only after project initialization and smart mode.
                    ProjectReadyScheduler.runWhenReady(project) {
                        if (route.canNavigate()) {
                            route.navigate(true)
                        }
                    }
                    return true
                }

                if (route.canNavigate()) {
                    route.navigate(true)
                    return true
                }

                return false
            }
        }.installOn(this)
    }

    override fun getData(dataId: String): Any? {
        // (Railways)
        // Good example of usage is in com.intellij.openapi.editor.impl.EditorComponentImpl (see getData method)

        if (PlatformDataKeys.SELECTED_ITEMS.`is`(dataId)) {
            return getSelectedRoutes()
        }

        if (PlatformDataKeys.SELECTED_ITEM.`is`(dataId)) {
            return getSelectedRoute()
        }

        return null
    }

    private fun getSelectedRoute(): BaseRoute? {
        val selectedId = convertRowIndexToModel(selectedRow)
        if (selectedId < 0) {
            return null
        }

        val m = model as RoutesTableModel
        return m.getRoute(selectedId)
    }

    private fun getSelectedRoutes(): Array<BaseRoute> {
        val m = model as RoutesTableModel

        val selectedRoutes = selectedRows.map { selectedRow ->
            m.getRoute(convertRowIndexToModel(selectedRow))
        }

        return selectedRoutes.toTypedArray()
    }

    inner class RailroadsPopupHandler: PopupHandler() {
        override fun mousePressed(e: MouseEvent?) {
            if (e != null) {
                handleRightClick(e)
            }
            super.mousePressed(e)
        }

        override fun mouseReleased(e: MouseEvent?) {
            if (e != null) {
                handleRightClick(e)
            }
            super.mouseReleased(e)
        }

        override fun invokePopup(comp: Component?, x: Int, y: Int) {
            val manager = ActionManager.getInstance()
            val group = ActionManager.getInstance().getAction("railroads.PopupMenu") as ActionGroup

            val popupMenu = manager.createActionPopupMenu(ActionPlaces.POPUP, group)
            popupMenu.component.show(comp, x, y)
        }

        private fun handleRightClick(e: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e) || !e.isPopupTrigger) {
                return
            }

            // (Railways)
            // Before showing the popup, we should update selection properly:
            //  * if right-clicked on existing selection - do nothing.
            //  * if clicked outside current selection - clear it and select
            //    only item that was clicked.
            val clickedRowIndex = rowAtPoint(e.point)
            val isSelectionClicked = ArrayUtil.indexOf(selectedRows, clickedRowIndex) >= 0

            if (!isSelectionClicked && clickedRowIndex >= 0 && clickedRowIndex < rowCount) {
                setRowSelectionInterval(clickedRowIndex, clickedRowIndex)
            }
        }
    }
}
