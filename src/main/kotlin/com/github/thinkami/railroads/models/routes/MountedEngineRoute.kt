package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.PsiUtil
import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.module.Module
import javax.swing.Icon

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

    override fun getActionIcon(): Icon {
        return RailroadIcon.NodeMountedEngine
    }
}