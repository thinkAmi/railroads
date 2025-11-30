package com.github.thinkami.railroads.workflow

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.plugins.ruby.rails.model.RailsApp

data class RailsContext(
    val module: Module,
    val appRoot: String,
    val sdk: Sdk
)

interface RailsContextProvider {
    suspend fun getContext(project: Project): RailsContext?

    class Default(private val readActionRunner: ReadActionRunner = ReadActionRunner.default()) : RailsContextProvider {
        override suspend fun getContext(project: Project): RailsContext? = readActionRunner.run {
            val module: Module = project.modules.firstOrNull() ?: return@run null
            val app = RailsApp.fromModule(module) ?: return@run null
            val sdk = ModuleRootManager.getInstance(module).sdk ?: return@run null
            val root = app.railsApplicationRoot?.presentableUrl ?: return@run null
            RailsContext(module, root, sdk)
        }
    }
}
