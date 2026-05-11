package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.ProjectReadyScheduler
import com.github.thinkami.railroads.helper.PsiUtil
import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
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
        val project = module.project

        ProjectReadyScheduler.runWhenReady(project) {
            // Precompute target VirtualFile and offset inside a background ReadAction.nonBlocking,
            // then open the file on the EDT. Mirrors SimpleRoute.navigate().
            ReadAction
                .nonBlocking<Pair<VirtualFile, Int>?> {
                    resolveNavigationTarget(mountedEngineController, project)
                }
                .inSmartMode(project)
                .expireWith(project)
                .finishOnUiThread(ModalityState.defaultModalityState()) { target ->
                    if (target != null) {
                        OpenFileDescriptor(project, target.first, target.second).navigate(requestFocus)
                    }
                }
                .submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    override fun getActionIcon(): Icon {
        return RailroadIcon.NodeMountedEngine
    }

    companion object {
        /**
         * Internal helper exposed for testing. Caller MUST hold a read action — the helper
         * asserts this on entry so that regressions where navigate() bypasses read action
         * are caught early. Returns null when the FQN cannot be resolved to an RContainer
         * or when the resolved element has no backing virtual file.
         */
        @JvmStatic
        internal fun resolveNavigationTarget(
            qualifiedName: String,
            project: Project
        ): Pair<VirtualFile, Int>? {
            ApplicationManager.getApplication().assertReadAccessAllowed()
            val element = PsiUtil.findClassOrModule(qualifiedName, project) ?: return null
            val vFile = element.containingFile?.virtualFile ?: return null
            val offset = element.textOffset
            return Pair(vFile, offset)
        }
    }
}
