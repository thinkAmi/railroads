package com.github.thinkami.railroads.workflow

import com.github.thinkami.railroads.coroutines.ProjectCoroutineScopeProvider
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface RoutesRunner {
    fun requestRoutes(project: Project)
}

class RoutesRunnerService(
    private val scopeProvider: ProjectCoroutineScopeProvider,
    private val processExecutor: RoutesProcessExecutor,
    private val uiCoordinator: RoutesUiCoordinator,
    private val progressRunner: ProgressRunner = ProgressRunner.withBackgroundProgressRunner(),
    private val errorReporter: RoutesErrorReporter = NotificationRoutesErrorReporter(),
    private val observability: RoutesObservability = RoutesLoggerObservability()
) : RoutesRunner {

    private var activeJob: Job? = null

    override fun requestRoutes(project: Project) {
        if (activeJob?.isActive == true) return
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            val message = formatUnexpectedError(throwable)
            errorReporter.notify(project, message, throwable)
            observability.onStateTransition(RouteLoadState.Error(message))
        }
        activeJob = scopeProvider.scope.launch(exceptionHandler) {
            uiCoordinator.update(RouteLoadState.Running)
            observability.onStateTransition(RouteLoadState.Running)
            if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread) {
                observability.onPotentialBlockingOperation("requestRoutes on EDT")
            }
            try {
                val result = progressRunner.run(project, TITLE) {
                    processExecutor.run(project)
                }
                val errorMessage = result.errorMessageOrNull()
                if (errorMessage != null) {
                    errorReporter.notify(project, errorMessage, null)
                    val state = RouteLoadState.Error(errorMessage)
                    uiCoordinator.update(state)
                    observability.onStateTransition(state)
                } else {
                    val state = RouteLoadState.Success(
                        result.routes,
                        result.stderr.takeIf { it.isNotBlank() })
                    uiCoordinator.update(state)
                    observability.onStateTransition(state)
                }
            } catch (t: Throwable) {
                val message = formatUnexpectedError(t)
                errorReporter.notify(project, message, t)
                val state = RouteLoadState.Error(message)
                uiCoordinator.update(state)
                observability.onStateTransition(state)
            } finally {
                activeJob = null
            }
        }
    }

    private fun ProcessResult.errorMessageOrNull(): String? {
        if (exitCode == 0 && stderr.isBlank()) return null
        val stderrMessage = stderr.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
        return "rails routes failed (exit $exitCode)$stderrMessage"
    }

    private fun formatUnexpectedError(t: Throwable): String =
        "rails routes failed: ${t.message ?: t::class.simpleName ?: "Unexpected error"}"

    companion object {
        private const val TITLE = "Running rails routes"
    }
}
