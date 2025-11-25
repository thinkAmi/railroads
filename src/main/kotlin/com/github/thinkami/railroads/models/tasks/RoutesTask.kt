package com.github.thinkami.railroads.models.tasks

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.github.thinkami.railroads.views.MainView
import com.intellij.execution.ExecutionModes
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext
import org.jetbrains.plugins.ruby.rails.model.RailsApp

class RoutesTask(private val project: Project) : Task.Backgroundable(project, "Running rails routes") {
    private val module: Module = project.modules.first()
    private var output: ProcessOutput = ProcessOutput()
    private var routes: List<BaseRoute> = listOf()

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return
        val mainView = MainView(toolWindow)
        mainView.renderLoadingWithUiThread()

        val app = RailsApp.fromModule(module)
        if ((app == null) || (app.railsApplicationRoot == null)) {
            mainView.renderDefaultWithUiThread()
            return
        }

        val moduleContentRoot = app.railsApplicationRoot!!.presentableUrl
        val manager = ModuleRootManager.getInstance(module)

        if (manager.sdk == null) {
            mainView.renderDefaultWithUiThread()
            return
        }
        val sdk = manager.sdk!!

        val result = RubyGemExecutionContext.create(sdk, "rails")
            .withModule(module)
            .withWorkingDirPath(moduleContentRoot)
            .withExecutionMode(ExecutionModes.SameThreadMode())
            .withArguments("routes", "--trace")
            .executeScript()

        if (result != null) {
            output = result
        }

        // Parse routes off the EDT to avoid slow PSI/index operations on UI thread
        if (output.stdout.isNotBlank()) {
            routes = RailsRoutesParser(module).parse(output.stdout)
        }
    }

    override fun onSuccess() {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return

        // (base comment: railways from parseRakeRouteOutput in RoutesManager.java)
        // After routes parsing we can have several situations:
        // 1. output.stdout is not blank and output.stderr is blank. Everything is OK.
        // 2. output.stdout is blank and output.stderr is not blank. It means that there was an exception thrown.
        // 3. output.stdout is not blank and output.stderr is not blank. In the most cases it's warnings (deprecation etc),
        //    so everything is OK.
        // TODO: possibly, we should report about warnings somehow.

        if (output.stdout.isBlank() && output.stderr.isNotBlank()) {
            val notification = Notification(
                "railroadsNotification",
                "Railroads Error: fail rails routes",
                output.stderr,
                NotificationType.ERROR
            )
            Notifications.Bus.notify(notification)
            MainView(toolWindow).renderErrorWithUiThread()
            return
        }

        MainView(toolWindow).renderRoutesWithUiThread(routes)

        ToolWindowManager.getInstance(project).notifyByBalloon(
            "Railroads",
            MessageType.INFO,
            "Finished rails routes"
        )
    }

    override fun onThrowable(error: Throwable) {
        println(output.stderr)
    }
}