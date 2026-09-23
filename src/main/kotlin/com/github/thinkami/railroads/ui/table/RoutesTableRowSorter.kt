package com.github.thinkami.railroads.ui.table

import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.methodPriority
import com.github.thinkami.railroads.models.routes.BaseRoute
import javax.swing.SortOrder
import javax.swing.table.TableRowSorter

class RoutesTableRowSorter(model: RoutesTableModel): TableRowSorter<RoutesTableModel>(model) {
    init {
        maxSortKeys = 1

        setComparator(METHOD_COLUMN, REQUEST_METHOD_COMPARATOR)
        setComparator(PATH_COLUMN, TEXT_COMPARATOR)
        setComparator(ACTION_COLUMN, ACTION_COMPARATOR)
        setComparator(NAME_COLUMN, TEXT_COMPARATOR)
    }

    // Cycles through ascending, descending and unsorted (the order of rails routes)
    override fun toggleSortOrder(column: Int) {
        val primarySortKey = sortKeys.firstOrNull()

        if (primarySortKey != null && primarySortKey.column == column && primarySortKey.sortOrder == SortOrder.DESCENDING) {
            sortKeys = emptyList()
            return
        }

        super.toggleSortOrder(column)
    }

    companion object {
        private const val METHOD_COLUMN = 0
        private const val PATH_COLUMN = 1
        private const val ACTION_COLUMN = 2
        private const val NAME_COLUMN = 3

        // In ascending order: GET, POST, PUT, PATCH, DELETE, other request methods, and then routes without a verb
        internal val REQUEST_METHOD_COMPARATOR: Comparator<String> =
            compareBy({ it.isEmpty() }, { methodPriority(it) }, { it })

        // In ascending order: case-insensitive order, and then empty values
        internal val TEXT_COMPARATOR: Comparator<String> =
            compareBy<String> { it.isEmpty() }.then(String.CASE_INSENSITIVE_ORDER)

        internal val ACTION_COMPARATOR: Comparator<BaseRoute> =
            compareBy(TEXT_COMPARATOR) { it.getActionTitle() }
    }
}
