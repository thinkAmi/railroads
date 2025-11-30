package com.github.thinkami.railroads.workflow

interface RoutesUiCoordinator {
    suspend fun update(state: RouteLoadState)
}
