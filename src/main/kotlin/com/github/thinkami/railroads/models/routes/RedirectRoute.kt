package com.github.thinkami.railroads.models.routes

import com.intellij.openapi.module.Module

class RedirectRoute(
    module: Module,
    requestMethod: String,
    routePath: String,
    routeName: String,
    redirectRoutePath: String?
): BaseRoute(module, requestMethod, routePath, routeName, controllerName = "", actionName = "") {
    private val redirectPath: String = redirectRoutePath ?: ""

    /**
     * (railways)
     * Returns displayable text for route action in short format. Short format
     * is used in routes table.
     *
     * @return Displayable text for route action, ex. "users#create"
     */
    override fun getActionTitle(): String {
        return redirectPath.ifBlank { "[redirect]" }
    }

    /**
     * (railways)
     * Returns displayable text for route action.
     *
     * @return Displayable text for route action, ex. "UsersController#create"
     */
    override fun getQualifiedActionTitle(): String {
        return if (redirectPath.isBlank()) "[runtime define redirect]" else "redirect to $redirectPath"
    }

}