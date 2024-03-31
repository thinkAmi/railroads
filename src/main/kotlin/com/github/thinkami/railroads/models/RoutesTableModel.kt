package com.github.thinkami.railroads.models

import com.github.thinkami.railroads.models.routes.BaseRoute
import javax.swing.table.AbstractTableModel

class RoutesTableModel: AbstractTableModel() {
    private var allRoutes: List<BaseRoute> = listOf()
    private var filteredRoutes: List<BaseRoute> = listOf()
    private val columns: List<String> = listOf("Method", "Path", "Action", "Name")
    val tableFilter: RoutesTableFilter = RoutesTableFilter(this)

    init {
        resourceChanged()
    }

    override fun getRowCount(): Int {
        return filteredRoutes.size
    }

    override fun getColumnCount(): Int {
        return columns.size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val route = filteredRoutes[rowIndex]

        // routeAction return route objects to use a custom renderer
        return when(columnIndex) {
            0 -> route.requestMethod
            1 -> route.routePath
            2 -> route
            3 -> route.routeName
            else -> {""}
        }
    }

    override fun getColumnName(columnIndex: Int): String {
        return columns[columnIndex]
    }

    fun updateTableDataFromRoutes(routes: List<BaseRoute>) {
        this.allRoutes = routes

        resourceChanged()
    }

    fun resourceChanged() {
        filteredRoutes = allRoutes.filter {
            tableFilter.match(it)
        }

        this.fireTableDataChanged()
    }

    fun getRoute(rowIndex: Int): BaseRoute {
        return filteredRoutes[rowIndex]
    }

    fun getTotalRoutesCount(): Int {
        return allRoutes.size
    }
}