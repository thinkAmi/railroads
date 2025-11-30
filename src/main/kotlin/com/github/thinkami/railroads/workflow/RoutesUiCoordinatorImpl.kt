package com.github.thinkami.railroads.workflow

import com.github.thinkami.railroads.coroutines.DispatcherSet
import com.github.thinkami.railroads.views.MainView
import kotlinx.coroutines.withContext

interface UiActions {
    fun renderLoading()
    fun renderRoutes(routes: List<com.github.thinkami.railroads.models.routes.BaseRoute>)
    fun renderError()

    companion object {
        fun fromMainView(view: MainView): UiActions = object : UiActions {
            override fun renderLoading() = view.renderLoadingWithUiThread()
            override fun renderRoutes(routes: List<com.github.thinkami.railroads.models.routes.BaseRoute>) =
                view.renderRoutesWithUiThread(routes)

            override fun renderError() = view.renderErrorWithUiThread()
        }
    }
}

class RoutesUiCoordinatorImpl(
    private val dispatchers: DispatcherSet,
    private val uiActionsProvider: () -> UiActions,
    private val observability: RoutesObservability = RoutesLoggerObservability()
) : RoutesUiCoordinator {
    override suspend fun update(state: RouteLoadState) = withContext(dispatchers.edt) {
        val ui = uiActionsProvider()
        val isDispatchThread = com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread
        observability.onUiDispatcherViolation(isDispatchThread, Thread.currentThread().name)
        observability.onStateTransition(state)
        when (state) {
            RouteLoadState.Running -> ui.renderLoading()
            is RouteLoadState.Success -> ui.renderRoutes(state.routes)
            is RouteLoadState.Error -> ui.renderError()
        }
    }
}
