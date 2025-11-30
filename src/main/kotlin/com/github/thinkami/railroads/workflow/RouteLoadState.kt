package com.github.thinkami.railroads.workflow

import com.github.thinkami.railroads.models.routes.BaseRoute

sealed class RouteLoadState {
    object Running : RouteLoadState()
    data class Success(val routes: List<BaseRoute>, val stderr: String?) : RouteLoadState()
    data class Error(val message: String) : RouteLoadState()
}
