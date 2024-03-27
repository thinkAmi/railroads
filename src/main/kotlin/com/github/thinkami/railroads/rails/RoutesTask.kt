package com.github.thinkami.railroads.rails

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
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext
import org.jetbrains.plugins.ruby.rails.model.RailsApp

class RoutesTask(private val project: Project) : Task.Backgroundable(project, "task start...") {
    private val module: Module = project.modules.first()
    private lateinit var output: ProcessOutput

    override fun run(indicator: ProgressIndicator) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return
        val mainView = MainView(toolWindow)
        mainView.renderLoadingWithUiThread()

        val app = RailsApp.fromModule(module)
        if ((app == null) || (app.railsApplicationRoot == null)) {
            mainView.renderDefaultWithUiThread()
            return
        }

        indicator.fraction = 0.1
        indicator.text = "Start"

        val moduleContentRoot = app.railsApplicationRoot!!.presentableUrl
        val manager = ModuleRootManager.getInstance(module)

        if (manager.sdk == null) {
            mainView.renderDefaultWithUiThread()
            return
        }
        val sdk = manager.sdk!!

        indicator.fraction = 0.5
        indicator.text = "Before rails routes"

        val result = RubyGemExecutionContext.create(sdk, "rails")
            .withModule(module)
            .withWorkingDirPath(moduleContentRoot)
            .withExecutionMode(ExecutionModes.SameThreadMode())
            .withArguments("routes", "--trace")
            .executeScript()

        indicator.fraction = 1.0
        indicator.text = "After rails routes"

        if (result != null) {
            output = result
        }
    }

    override fun onSuccess() {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return

        if (output.stderr.isNotBlank()) {
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

        val routes = RailsRoutesParser(module).parse(output.stdout)
        MainView(toolWindow).renderRoutesWithUiThread(routes)
    }

    override fun onThrowable(error: Throwable) {
        println(output.stderr)
    }
}