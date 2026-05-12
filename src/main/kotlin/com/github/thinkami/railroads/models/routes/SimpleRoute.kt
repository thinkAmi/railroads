package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.ProjectReadyScheduler
import com.github.thinkami.railroads.helper.PsiUtil
import com.github.thinkami.railroads.models.RailsAction
import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
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

    // Snapshot fields. Captured at init time and read by getXxx() so UI / renderer / filter can
    // call those methods from the EDT without holding a read action.
    // Default values are aligned with the "PSI not yet resolved" fallback so that any accidental
    // observation before init completes still produces safe values.
    private var actionTitleSnapshot: String = ""
    private var qualifiedActionTitleSnapshot: String = ""
    private var canNavigateSnapshot: Boolean = false
    private var methodExistsSnapshot: Boolean = false
    private var actionIconSnapshot: Icon = RailroadIcon.Unknown

    init {
        // Run RailsAction.update() and the snapshot computation inside the same read action so
        // that PSI is observed at a consistent point in time. RailsAction.update() also wraps
        // in ReadAction.run internally; nested read actions are safe in the IntelliJ Platform.
        ReadAction.run<RuntimeException> {
            railsAction.update(module, controllerName, actionName)
            recomputeSnapshotInternal()
        }
    }

    // Computation body. Reads PSI via railsAction; must be called inside a read action.
    private fun recomputeSnapshotInternal() {
        actionTitleSnapshot = if (controllerName.isNotBlank()) {
            "$controllerName#$actionName"
        } else {
            actionName
        }

        qualifiedActionTitleSnapshot = computeQualifiedActionTitle()
        canNavigateSnapshot = railsAction.psiMethod != null || railsAction.psiClass != null
        methodExistsSnapshot = railsAction.psiMethod != null
        actionIconSnapshot = computeActionIcon()
    }

    private fun computeQualifiedActionTitle(): String {
        // (railways)
        // Return unqualified action title in case controller is specified as
        // parameter (ex. :controller#:action)
        if (controllerName.contains(":")) {
            return actionTitleSnapshot
        }

        val controllerClass = railsAction.psiClass
        val controllerClassName = if (controllerClass != null) {
            controllerClass.fqnWithNesting.fullPath
        } else {
            PsiUtil.getControllerClassNameByShortName(controllerName)
        }

        return "$controllerClassName#$actionName"
    }

    private fun computeActionIcon(): Icon {
        return resolveActionIcon(
            hasMethod = railsAction.psiMethod != null,
            hasClass = railsAction.psiClass != null,
            methodIconProvider = { railsAction.getIcon() }
        )
    }

    companion object {
        /**
         * Pure dispatch helper for action icon selection. Extracted so unit tests can exercise
         * each branch without instantiating real Ruby PSI (which is non-trivial in
         * BasePlatformTestCase because the Ruby plugin requires several optional services).
         */
        @JvmStatic
        internal fun resolveActionIcon(
            hasMethod: Boolean,
            hasClass: Boolean,
            methodIconProvider: () -> Icon,
        ): Icon {
            if (hasMethod) return methodIconProvider()
            if (hasClass) return RailroadIcon.NodeController
            return RailroadIcon.Unknown
        }
    }

    override fun navigate(requestFocus: Boolean) {
        val project = module.project

        ProjectReadyScheduler.runWhenReady(project) {
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
    }

    override fun canNavigate(): Boolean {
        return canNavigateSnapshot
    }

    override fun methodExists(): Boolean {
        return methodExistsSnapshot
    }

    override fun getActionTitle(): String {
        return actionTitleSnapshot
    }

    override fun getQualifiedActionTitle(): String {
        return qualifiedActionTitleSnapshot
    }

    override fun getActionIcon(): Icon {
        return actionIconSnapshot
    }
}
