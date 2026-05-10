package com.github.thinkami.railroads.models.tasks

import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.plugins.ruby.rails.model.RailsApp

internal sealed interface RoutesPreflightResult {
    data object NoModule : RoutesPreflightResult
    data object NotRailsApplication : RoutesPreflightResult
    data object MissingRubySdk : RoutesPreflightResult
    data class Ready(
        val module: Module,
        val moduleContentRoot: String,
        val sdk: Sdk
    ) : RoutesPreflightResult
}

internal enum class RoutesPreflightResultKind {
    NoModule, NotRailsApplication, MissingRubySdk, Ready
}

internal fun decideRoutesPreflight(
    hasModule: Boolean,
    hasRailsRoot: Boolean,
    hasSdk: Boolean
): RoutesPreflightResultKind {
    if (!hasModule) return RoutesPreflightResultKind.NoModule
    if (!hasRailsRoot) return RoutesPreflightResultKind.NotRailsApplication
    if (!hasSdk) return RoutesPreflightResultKind.MissingRubySdk
    return RoutesPreflightResultKind.Ready
}

// RailsApp.fromModule and railsApplicationRoot.presentableUrl touch the PSI/Project model,
// so they must be called inside a read action (see RailsAction.kt).
internal suspend fun resolveRoutesPreflight(project: Project): RoutesPreflightResult =
    readAction {
        val module = project.modules.firstOrNull()
        val app = module?.let { RailsApp.fromModule(it) }
        val root = app?.railsApplicationRoot
        val sdk = module?.let { ModuleRootManager.getInstance(it).sdk }

        when (decideRoutesPreflight(
            hasModule = module != null,
            hasRailsRoot = root != null,
            hasSdk = sdk != null
        )) {
            RoutesPreflightResultKind.NoModule -> RoutesPreflightResult.NoModule
            RoutesPreflightResultKind.NotRailsApplication -> RoutesPreflightResult.NotRailsApplication
            RoutesPreflightResultKind.MissingRubySdk -> RoutesPreflightResult.MissingRubySdk
            RoutesPreflightResultKind.Ready -> RoutesPreflightResult.Ready(
                module = module!!,
                moduleContentRoot = root!!.presentableUrl,
                sdk = sdk!!
            )
        }
    }
