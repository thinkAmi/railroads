package com.github.thinkami.railroads.models

data class MethodFilterItem(val label: String, val requestMethod: String?) {
    override fun toString(): String {
        return label
    }

    companion object {
        val ALL = MethodFilterItem("All", null)

        // Routes without a verb (e.g. mounted Rack apps) have an empty request method
        val BLANK = MethodFilterItem("(blank)", "")
    }
}

private val PRIORITY_METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE")

// Builds combo box items in the order: All, request methods, (blank).
// The selected item is kept in the items even if no route has its request method.
fun buildMethodFilterItems(methods: List<String>, selected: MethodFilterItem?): List<MethodFilterItem> {
    val selectedMethod = selected?.requestMethod
    val candidates = if (selectedMethod == null) methods else methods + selectedMethod

    val requestMethods = candidates
        .filter { it.isNotEmpty() }
        .distinct()
        .sortedWith(compareBy({ methodPriority(it) }, { it }))
        .map { MethodFilterItem(it, it) }

    val blank = if (candidates.contains("")) listOf(MethodFilterItem.BLANK) else listOf()

    return listOf(MethodFilterItem.ALL) + requestMethods + blank
}

private fun methodPriority(method: String): Int {
    val index = PRIORITY_METHODS.indexOf(method)
    return if (index >= 0) index else PRIORITY_METHODS.size
}
