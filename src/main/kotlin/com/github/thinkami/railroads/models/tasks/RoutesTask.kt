package com.github.thinkami.railroads.models.tasks

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.github.thinkami.railroads.views.MainView
import com.intellij.execution.ExecutionModes
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.progress.ProgressManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.thinkami.railroads.services.RoutesCoroutineService
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext

// Coroutine-based route retrieval process.
// Does not display progress dialogs or cancellation options.
fun launchRoutes(project: Project) {
    // Launch with scope aligned to the project's lifecycle
    val service = project.getService(RoutesCoroutineService::class.java)
    val scope = service.scope
    scope.launch {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return@launch

        // MainView's init walks the Swing component tree, so it must be constructed on the EDT.
        val mainView = withContext(Dispatchers.EDT) {
            MainView(toolWindow).also { it.renderLoadingWithUiThread() }
        }

        val ready: RoutesPreflightResult.Ready = when (val result = resolveRoutesPreflight(project)) {
            RoutesPreflightResult.NoModule -> {
                notifyConfigurationIssueOnEdt(
                    project, mainView,
                    "Railroads: No module found",
                    "This project has no modules. Open a project containing a Rails application."
                )
                return@launch
            }
            RoutesPreflightResult.NotRailsApplication -> {
                notifyConfigurationIssueOnEdt(
                    project, mainView,
                    "Railroads: Not a Rails project",
                    "No Rails application was found. Mark the Rails app directory as Ruby Module Root in RubyMine."
                )
                return@launch
            }
            RoutesPreflightResult.MultipleRailsApplications -> {
                notifyConfigurationIssueOnEdt(
                    project, mainView,
                    "Railroads: Multiple Rails applications found",
                    "Railroads supports a single Rails application per project. Open one Rails app or keep only one Ruby Module Root enabled."
                )
                return@launch
            }
            RoutesPreflightResult.MissingRubySdk -> {
                notifyConfigurationIssueOnEdt(
                    project, mainView,
                    "Railroads: Ruby interpreter is not configured",
                    "Configure the Ruby SDK in Project Structure → Modules → SDK to run rails routes."
                )
                return@launch
            }
            is RoutesPreflightResult.Ready -> result
        }

        val module = ready.module
        val railsApplicationRoot = ready.railsApplicationRoot
        val sdk = ready.sdk

        val output: ProcessOutput = try {
            var result: ProcessOutput? = null
            withContext(Dispatchers.IO) {
                ProgressManager.getInstance().executeNonCancelableSection(Runnable {
                    result = RubyGemExecutionContext.create(sdk, "rails")
                        .withModule(module)
                        .withWorkingDirPath(railsApplicationRoot)
                        .withExecutionMode(ExecutionModes.SameThreadMode())
                        .withArguments("routes", "--trace")
                        .executeScript() ?: ProcessOutput()
                })
            }
            result ?: ProcessOutput()
        } catch (t: Throwable) {
            withContext(Dispatchers.EDT) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("railroadsNotification")
                    .createNotification(
                        "Railroads Error: fail rails routes",
                        t.message ?: "",
                        NotificationType.ERROR
                    )
                    .notify(project)
                mainView.renderErrorWithUiThread()
            }
            return@launch
        }

        val routes: List<BaseRoute> = if (output.stdout.isNotBlank()) {
            // Protect analysis that may modify PSI/Index with readAction
            readAction { RailsRoutesParser(module).parse(output.stdout) }
        } else emptyList()

        withContext(Dispatchers.EDT) {
            if (output.stdout.isBlank() && output.stderr.isNotBlank()) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("railroadsNotification")
                    .createNotification(
                        "Railroads Error: fail rails routes",
                        output.stderr,
                        NotificationType.ERROR
                    ).notify(project)
                mainView.renderErrorWithUiThread()
                return@withContext
            }

            mainView.renderRoutesWithUiThread(routes)

            ToolWindowManager.getInstance(project).notifyByBalloon(
                "Railroads",
                MessageType.INFO,
                "Finished rails routes"
            )
        }
    }
}

private suspend fun notifyConfigurationIssueOnEdt(
    project: Project,
    mainView: MainView,
    title: String,
    message: String
) {
    withContext(Dispatchers.EDT) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("railroadsNotification")
            .createNotification(title, message, NotificationType.WARNING)
            .notify(project)
        mainView.renderConfigurationIssueWithUiThread(message)
    }
}
