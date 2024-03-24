package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.PsiUtil
import com.intellij.openapi.module.Module

class MountedEngineRoute(
    module: Module,
    requestMethod: String,
    routePath: String,
    routeName: String,
    private val mountedEngineController: String
): BaseRoute(module, requestMethod, routePath, routeName, controllerName = "", actionName = "") {
    override fun getQualifiedActionTitle(): String {
        return mountedEngineController
    }

    override fun navigate(requestFocus: Boolean) {
        PsiUtil.findClassOrModule(mountedEngineController, module.project)?.navigate(requestFocus)
    }
}