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
    data object MultipleRailsApplications : RoutesPreflightResult
    data object MissingRubySdk : RoutesPreflightResult
    data class Ready(
        val module: Module,
        val railsApplicationRoot: String,
        val sdk: Sdk
    ) : RoutesPreflightResult
}

internal enum class RoutesPreflightResultKind {
    NoModule, NotRailsApplication, MultipleRailsApplications, MissingRubySdk, Ready
}

internal fun decideRoutesPreflight(
    moduleCount: Int,
    railsApplicationCount: Int,
    hasSdk: Boolean
): RoutesPreflightResultKind {
    if (moduleCount == 0) return RoutesPreflightResultKind.NoModule
    if (railsApplicationCount == 0) return RoutesPreflightResultKind.NotRailsApplication
    if (railsApplicationCount > 1) return RoutesPreflightResultKind.MultipleRailsApplications
    if (!hasSdk) return RoutesPreflightResultKind.MissingRubySdk
    return RoutesPreflightResultKind.Ready
}

private data class RailsApplicationCandidate(
    val module: Module,
    val rootPath: String,
    val sdk: Sdk?
)

// RailsApp.fromModule and railsApplicationRoot.presentableUrl touch the PSI/Project model,
// so they must be called inside a read action (see RailsAction.kt).
internal suspend fun resolveRoutesPreflight(project: Project): RoutesPreflightResult =
    readAction {
        val modules = project.modules
        val candidates = modules.mapNotNull { module ->
            val root = RailsApp.fromModule(module)?.railsApplicationRoot ?: return@mapNotNull null
            RailsApplicationCandidate(
                module = module,
                rootPath = root.presentableUrl,
                sdk = ModuleRootManager.getInstance(module).sdk
            )
        }
        val candidate = candidates.singleOrNull()

        when (decideRoutesPreflight(
            moduleCount = modules.size,
            railsApplicationCount = candidates.size,
            hasSdk = candidate?.sdk != null
        )) {
            RoutesPreflightResultKind.NoModule -> RoutesPreflightResult.NoModule
            RoutesPreflightResultKind.NotRailsApplication -> RoutesPreflightResult.NotRailsApplication
            RoutesPreflightResultKind.MultipleRailsApplications -> RoutesPreflightResult.MultipleRailsApplications
            RoutesPreflightResultKind.MissingRubySdk -> RoutesPreflightResult.MissingRubySdk
            RoutesPreflightResultKind.Ready -> RoutesPreflightResult.Ready(
                module = candidate!!.module,
                railsApplicationRoot = candidate.rootPath,
                sdk = candidate.sdk!!
            )
        }
    }
