package com.github.thinkami.railroads.ui

import com.github.thinkami.railroads.actions.RailsRouteAction
import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.SearchTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
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
    private lateinit var pathFilter: Cell<SearchTextField>
    private lateinit var routesCounter: Cell<JLabel>
    private var currentRoute: BaseRoute? = null

    var content: DialogPanel

    init {
        content = panel {
            row {
                button("Rails Routes", RailsRouteAction())

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

                routesCounter = label("")
                routesCounter.component.name = "routesCounter"
            }

            row {
                table = JBTable()
                table.model = routesTableModel
                table.autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
                table.fillsViewportHeight = true

                // Adjust the width and height of the JBTable automatically
                scrollCell(table).align(AlignX.FILL).align(AlignY.FILL)

                // Event: Reflect the selected row to each label
                table.selectionModel.addListSelectionListener {
                    if (!it.valueIsAdjusting) {
                        val originalRowIndex = table.convertRowIndexToModel(table.selectedRow)
                        val model = table.model as RoutesTableModel
                        val route = if (originalRowIndex >= 0) model.getRoute(originalRowIndex) else null

                        showRoute(route)
                    }
                }

                table.model.addTableModelListener {
                    updateCounter()
                }
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
        }
    }

    private fun handleFilterChange() {
        routesTableModel.tableFilter.filterText = pathFilter.component.text
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