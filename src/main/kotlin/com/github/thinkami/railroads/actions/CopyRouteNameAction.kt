package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.models.routes.BaseRoute

class CopyRouteNameAction: CopyRouteActionBase() {
    override fun getRouteValue(route: BaseRoute): String {
        return route.routeName
    }
}