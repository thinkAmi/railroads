package com.github.thinkami.railroads.models

class RoutesTableFilter(val model: RoutesTableModel) {
    var filterText: String = ""

        set(value) {
            if (value != filterText) {
                field = value
                model.resourceChanged()
            }
        }
}