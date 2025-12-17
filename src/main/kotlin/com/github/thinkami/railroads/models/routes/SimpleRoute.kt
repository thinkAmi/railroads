package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.PsiUtil
import com.github.thinkami.railroads.models.RailsAction
import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
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
        val project = module.project

        // Perform navigation only after the project is fully opened
        if (!project.isInitialized) {
            StartupManager.getInstance(project).runAfterOpened {
                navigate(requestFocus)
            }
            return
        }

        // To avoid heavy work on the EDT, precompute the target VirtualFile and offset
        // in a background ReadAction.nonBlocking, and open the file on the EDT only.
        ReadAction
            .nonBlocking<Pair<VirtualFile, Int>?> {
                // Complete index/PSI lookups inside the background ReadAction
                railsAction.update(module, controllerName, actionName)
                val method = railsAction.psiMethod
                val clazz = railsAction.psiClass

                // Priority: method -> class
                val element = method ?: clazz ?: return@nonBlocking null
                val vFile = element.containingFile?.virtualFile ?: return@nonBlocking null
                val offset = element.textOffset
                Pair(vFile, offset)
            }
            .inSmartMode(project) // Do not execute in Dumb mode
            .expireWith(project)  // Cancel when the project is disposed
            .finishOnUiThread(ModalityState.defaultModalityState()) { target ->
                if (target != null) {
                    OpenFileDescriptor(project, target.first, target.second).navigate(requestFocus)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
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
        val controllerClassName = if (controllerClass != null) controllerClass.fqnWithNesting.fullPath else PsiUtil.getControllerClassNameByShortName(controllerName)

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