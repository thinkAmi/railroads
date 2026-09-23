package com.github.thinkami.railroads.ui

import com.github.thinkami.railroads.actions.RailsRouteAction
import com.github.thinkami.railroads.models.MethodFilterItem
import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.buildMethodFilterItems
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.ui.table.ActionCellRenderer
import com.github.thinkami.railroads.ui.table.RoutesTable
import com.github.thinkami.railroads.ui.table.RoutesTableRowSorter
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.SearchTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class MainContent {
    private val routesTableModel = RoutesTableModel()
    private lateinit var table: JBTable
    private lateinit var routePath: Cell<JLabel>
    private lateinit var methodName: Cell<JLabel>
    private lateinit var actionName: Cell<HyperlinkLabel>
    private lateinit var routeName: Cell<JLabel>
    private lateinit var methodFilter: Cell<ComboBox<MethodFilterItem>>
    private lateinit var pathFilter: Cell<SearchTextField>
    private lateinit var routesCounter: Cell<JLabel>
    private lateinit var runRailsRoutesMessage: Cell<JLabel>
    private lateinit var loadingMessage: Cell<JLabel>
    private lateinit var raiseErrorMessage: Cell<JLabel>
    private lateinit var configurationIssueMessage: Cell<JLabel>
    private var currentRoute: BaseRoute? = null

    var content: DialogPanel

    init {
        content = panel {
            row {
                button("Rails Routes", RailsRouteAction())

                methodFilter = comboBox(listOf(MethodFilterItem.ALL))
                methodFilter.component.name = "methodFilter"
                methodFilter.component.addItemListener {
                    if (it.stateChange == ItemEvent.SELECTED) {
                        handleMethodFilterChange()
                    }
                }

                pathFilter = cell(SearchTextField())
                pathFilter.component.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(p0: DocumentEvent?) {
                        handleFilterChange()
                    }

                    override fun removeUpdate(p0: DocumentEvent?) {
                        handleFilterChange()
                    }

                    override fun changedUpdate(p0: DocumentEvent?) {
                        handleFilterChange()
                    }

                })
                pathFilter.align(AlignX.FILL).resizableColumn()

                routesCounter = label("--/--")
                routesCounter.component.name = "routesCounter"
            }

            row {
                table = RoutesTable()
                table.model = routesTableModel
                table.rowSorter = RoutesTableRowSorter(routesTableModel)
                table.autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
                table.fillsViewportHeight = true

                // Adjust the width and height of the JBTable automatically
                scrollCell(table).align(AlignX.FILL).align(AlignY.FILL).visible(false)

                // Event: Reflect the selected row to each label
                table.selectionModel.addListSelectionListener {
                    if (!it.valueIsAdjusting) {
                        // convertRowIndexToModel throws for -1 (no selection) when a row sorter is set
                        val selectedRow = table.selectedRow
                        val model = table.model as RoutesTableModel
                        val route = if (selectedRow >= 0) model.getRoute(table.convertRowIndexToModel(selectedRow)) else null

                        showRoute(route)
                    }
                }

                table.model.addTableModelListener {
                    updateCounter()
                }

                // add custom renderer for icon and color
                table.columnModel.getColumn(2).cellRenderer = ActionCellRenderer()

            }.resizableRow()

            row {
                label("Route").gap(RightGap.COLUMNS)
                routePath = label("")
            }.layout(RowLayout.LABEL_ALIGNED).visible(false)

            row {
                label("Method").gap(RightGap.COLUMNS)
                methodName = label("")
            }.layout(RowLayout.LABEL_ALIGNED).visible(false)

            row {
                label("Action").gap(RightGap.COLUMNS)
                actionName = cell(HyperlinkLabel(""))
                actionName.component.addHyperlinkListener {
                    currentRoute?.navigate(false)
                }

            }.layout(RowLayout.LABEL_ALIGNED).visible(false)

            row {
                label("Name").gap(RightGap.COLUMNS)
                routeName = label("")
            }.layout(RowLayout.LABEL_ALIGNED).visible(false)

            row {
                runRailsRoutesMessage = label("Please run rails routes").align(AlignX.CENTER).align(AlignY.CENTER)
                runRailsRoutesMessage.component.name = "runRailsRoutesMessage"
            }.topGap(TopGap.MEDIUM).bottomGap(BottomGap.MEDIUM).resizableRow()

            row {
                loadingMessage = label("Loading...").align(AlignX.CENTER).align(AlignY.CENTER)
                loadingMessage.component.name = "loadingMessage"
            }.topGap(TopGap.MEDIUM).bottomGap(BottomGap.MEDIUM).resizableRow().visible(false)

            row {
                raiseErrorMessage = label("An error has occurred, please check Notification").align(AlignX.CENTER).align(AlignY.CENTER)
                raiseErrorMessage.component.name = "raiseErrorMessage"
            }.topGap(TopGap.MEDIUM).bottomGap(BottomGap.MEDIUM).resizableRow().visible(false)

            row {
                configurationIssueMessage = label("").align(AlignX.CENTER).align(AlignY.CENTER)
                configurationIssueMessage.component.name = "configurationIssueMessage"
            }.topGap(TopGap.MEDIUM).bottomGap(BottomGap.MEDIUM).resizableRow().visible(false)
        }

        routesTableModel.onRoutesUpdated = {
            rebuildMethodFilterItems()
        }
    }

    private fun handleFilterChange() {
        routesTableModel.tableFilter.filterText = pathFilter.component.text
    }

    private fun handleMethodFilterChange() {
        val item = methodFilter.component.selectedItem as? MethodFilterItem ?: return
        routesTableModel.tableFilter.requestMethod = item.requestMethod
    }

    private fun rebuildMethodFilterItems() {
        val selected = methodFilter.component.selectedItem as? MethodFilterItem ?: MethodFilterItem.ALL
        val items = buildMethodFilterItems(routesTableModel.getRequestMethods(), selected)

        // Replacing the model keeps the selection without firing item events
        val model = DefaultComboBoxModel(items.toTypedArray())
        model.selectedItem = selected
        methodFilter.component.model = model
    }

    private fun showRoute(route: BaseRoute?) {
        currentRoute = route

        if (route == null) {
            methodName.component.text = ""
            routePath.component.text = ""
            actionName.component.setHyperlinkText("")
            routeName.component.text = ""
        } else {
            methodName.component.text = route.requestMethod
            routePath.component.text = route.routePath
            routeName.component.text = route.routeName

            if (route.canNavigate()) {
                actionName.component.setHyperlinkText(route.getQualifiedActionTitle())
            } else {
                actionName.component.setText(route.getQualifiedActionTitle())
            }
        }
    }

    private fun updateCounter() {
        routesCounter.component.text = "${routesTableModel.rowCount}/${routesTableModel.getTotalRoutesCount()}"
    }
}