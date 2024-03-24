package com.github.thinkami.railroads.models.routes

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.module.Module
import javax.swing.Icon

open class BaseRoute(
    val module: Module,
    val requestMethod: String,
    var routePath: String,
    var routeName: String,
    var controllerName: String,
    var actionName: String
): NavigationItem {
    override fun getName(): String? {
        return routePath
    }

    override fun getPresentation(): ItemPresentation? {
        val route = this

        return object : ItemPresentation{
            override fun getPresentableText(): String? {
                return actionName
            }

            override fun getIcon(unused: Boolean): Icon? {
                // TODO iconはまだない
                return null
            }

        }
    }

    /**
     * (railways)
     * Returns displayable text for route action in short format. Short format
     * is used in routes table.
     *
     * @return Displayable text for route action, ex. "users#create"
     */
    open fun getActionTitle(): String {
        return getQualifiedActionTitle()
    }

    /**
     * (railways)
     * Returns qualified name for route action.
     *
     * @return Displayable text for route action, ex. "UsersController#create"
     */
    open fun getQualifiedActionTitle(): String {
        return ""
    }

    override fun canNavigate(): Boolean  {
        return false
    }

}