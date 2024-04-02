package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.PsiUtil
import com.github.thinkami.railroads.models.RailsAction
import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.module.Module
import javax.swing.Icon

class SimpleRoute(
    module: Module,
    requestMethod: String,
    routePath: String,
    routeName: String,
    controllerName: String,
    actionName: String
): BaseRoute(module, requestMethod, routePath, routeName, controllerName, actionName) {
    private val railsAction = RailsAction()

    init {
        railsAction.update(module, controllerName, actionName)
    }

    override fun navigate(requestFocus: Boolean) {
        railsAction.update(module, controllerName, actionName)

        if (railsAction.psiMethod != null) {
            railsAction.psiMethod!!.navigate(requestFocus)
        } else if (railsAction.psiClass != null) {
            railsAction.psiClass!!.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean {
        return railsAction.psiMethod != null || railsAction.psiClass != null
    }

    override fun methodExists(): Boolean {
        return railsAction.psiMethod != null
    }

    override fun getActionTitle(): String {
        if (controllerName.isNotBlank()) {
            return "$controllerName#$actionName"
        }
        return actionName
    }

    override fun getQualifiedActionTitle(): String {
        // (railways)
        // Return unqualified action title in case controller is specified as
        // parameter (ex. :controller#:action)
        if (controllerName.contains(":")) {
            return getActionTitle()
        }

        val controllerClass = railsAction.psiClass
        val controllerClassName = if (controllerClass != null) controllerClass.qualifiedName else PsiUtil.getControllerClassNameByShortName(controllerName)

        return "$controllerClassName#$actionName"
    }

    override fun getActionIcon(): Icon {
        if (railsAction.psiMethod != null) {
            return railsAction.getIcon()
        }

        if (railsAction.psiClass != null) {
            return RailroadIcon.NodeController
        }

        return RailroadIcon.Unknown
    }
}