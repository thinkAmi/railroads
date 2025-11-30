package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.coroutines.ProjectCoroutineScopeProvider
import com.github.thinkami.railroads.workflow.*
import com.github.thinkami.railroads.views.MainView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class RailsRouteAction(
    private val runnerFactory: (com.intellij.openapi.project.Project) -> RoutesRunner? = runnerFactory@{ project ->
        val scopeProvider = project.getService(ProjectCoroutineScopeProvider::class.java) ?: return@runnerFactory null
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return@runnerFactory null
        val mainView = MainView(toolWindow)
        val uiCoordinator: RoutesUiCoordinator = RoutesUiCoordinatorImpl(
            dispatchers = scopeProvider.dispatcherSet(),
            uiActionsProvider = { UiActions.fromMainView(mainView) }
        )
        val executor: RoutesProcessExecutor = RoutesProcessExecutorImpl(
            railsContextProvider = RailsContextProvider.Default(),
            commandRunner = RubyRoutesCommandRunner()
        )
        RoutesRunnerService(
            scopeProvider = scopeProvider,
            processExecutor = executor,
            uiCoordinator = uiCoordinator,
            progressRunner = ProgressRunner.withBackgroundProgressRunner()
        )
    }
) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val runner = runnerFactory(project) ?: return
        runner.requestRoutes(project)
    }
}
