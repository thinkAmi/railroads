package com.github.thinkami.railroads.helper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

object ProjectReadyScheduler {
    fun isReady(project: Project): Boolean {
        return !project.isDisposed && project.isInitialized && !DumbService.isDumb(project)
    }

    fun runWhenReady(project: Project, task: () -> Unit) {
        if (project.isDisposed) {
            return
        }

        if (!isReady(project)) {
            DumbService.getInstance(project).runWhenSmart {
                if (project.isDisposed) {
                    return@runWhenSmart
                }

                if (isReady(project)) {
                    task()
                } else {
                    ApplicationManager.getApplication().invokeLater(
                        { runWhenReady(project, task) },
                        ModalityState.any()
                    )
                }
            }
            return
        }

        task()
    }
}
