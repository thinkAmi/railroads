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
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.progress.ProgressManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.thinkami.railroads.services.RoutesCoroutineService
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext
import org.jetbrains.plugins.ruby.rails.model.RailsApp

// Coroutine-based route retrieval process.
// Does not display progress dialogs or cancellation options.
fun launchRoutes(project: Project) {
    // Launch with scope aligned to the project's lifecycle
    val service = project.getService(RoutesCoroutineService::class.java)
    val scope = service.scope
    scope.launch {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return@launch
        val mainView = MainView(toolWindow)

        // UI: Loading indicator (EDT)
        withContext(Dispatchers.EDT) {
            mainView.renderLoadingWithUiThread()
        }

        val module = project.modules.firstOrNull()
        if (module == null) {
            withContext(Dispatchers.EDT) { mainView.renderDefaultWithUiThread() }
            return@launch
        }

        val app = RailsApp.fromModule(module)
        if (app?.railsApplicationRoot == null) {
            withContext(Dispatchers.EDT) { mainView.renderDefaultWithUiThread() }
            return@launch
        }

        val moduleContentRoot = app.railsApplicationRoot!!.presentableUrl
        val manager = ModuleRootManager.getInstance(module)

        val sdk = manager.sdk
        if (sdk == null) {
            withContext(Dispatchers.EDT) { mainView.renderDefaultWithUiThread() }
            return@launch
        }

        val output: ProcessOutput = try {
            var result: ProcessOutput? = null
            withContext(Dispatchers.IO) {
                ProgressManager.getInstance().executeNonCancelableSection(Runnable {
                    result = RubyGemExecutionContext.create(sdk, "rails")
                        .withModule(module)
                        .withWorkingDirPath(moduleContentRoot)
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