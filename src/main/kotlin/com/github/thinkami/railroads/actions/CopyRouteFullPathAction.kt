package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.models.routes.BaseRoute

class CopyRouteFullPathAction: CopyRouteActionBase() {
    override fun getRouteValue(route: BaseRoute): String {
        return route.getRoutePathWithMethod()
    }
}