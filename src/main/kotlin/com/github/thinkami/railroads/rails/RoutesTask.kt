package com.github.thinkami.railroads.rails

import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.execution.ExecutionModes
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import com.intellij.ui.table.JBTable
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext
import org.jetbrains.plugins.ruby.rails.model.RailsApp

class RoutesTask(private val project: Project) : Task.Backgroundable(project, "task start...") {
    private val module: Module = project.modules.first()
    private lateinit var output: ProcessOutput

    override fun run(indicator: ProgressIndicator) {
        val app = RailsApp.fromModule(module)
        if ((app == null) || (app.railsApplicationRoot == null)) {
            return
        }

        indicator.fraction = 0.1
        indicator.text = "Start"

        val moduleContentRoot = app.railsApplicationRoot!!.presentableUrl
        val manager = ModuleRootManager.getInstance(module)
        val sdk = manager.sdk ?: return

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

        println(output.stdout)
    }

    override fun onSuccess() {
        val result = RailsRoutesParser(module).parse(output.stdout)

        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Railroads") ?: return@invokeLater
            val panelComponent = toolWindow.component.components.filterIsInstance<DialogPanel>().first()
            val scrollComponent = panelComponent.components.filterIsInstance<JBScrollPane>().first()
            val viewportComponents = scrollComponent.components.filterIsInstance<JBViewport>().filter {
                it.components.filterIsInstance<JBTable>().isNotEmpty()
            }
            val viewportComponent = viewportComponents.first()
            val tableComponent = viewportComponent.components.filterIsInstance<JBTable>().first()
            val model = tableComponent.model as RoutesTableModel
            model.updateTableDataFromRoutes(result)
        }
    }
}