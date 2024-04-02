package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.models.routes.BaseRoute

class CopyRoutePathAction: CopyRouteActionBase() {
    override fun getRouteValue(route: BaseRoute): String {
        return route.getRoutePathWithoutRequestFormat()
    }
}