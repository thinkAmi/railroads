package com.github.thinkami.railroads.models

import com.github.thinkami.railroads.models.routes.BaseRoute
import java.util.*
import java.util.regex.Pattern

class RoutesTableFilter(val model: RoutesTableModel) {
    var filterText: String = ""
        set(value) {
            val newFilterText = value.lowercase(Locale.getDefault())

            if (newFilterText != filterText) {
                field = newFilterText
                filterPattern = buildFilterPattern(newFilterText)

                model.resourceChanged()
            }
        }

    // null matches all routes, and an empty string matches routes without a verb
    var requestMethod: String? = null
        set(value) {
            if (value != requestMethod) {
                field = value

                model.resourceChanged()
            }
        }

    private var filterPattern = Pattern.compile("")

    private fun buildFilterPattern(filterText: String): Pattern {
        val replacedFilterText = filterText.replace("[\\\\.\\[\\]{}()+\\-?^$|]", "\\\\$0")
        val regex = replacedFilterText.replace("*", ".*?")
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
    }
    fun match(route: BaseRoute): Boolean {
        val isRequestMethodMatched = requestMethod == null || route.requestMethod == requestMethod

        return isRequestMethodMatched && (
                filterPattern.matcher(route.routePath).find() ||
                filterPattern.matcher(route.getActionTitle()).find() ||
                filterPattern.matcher(route.routeName).find())
    }
}