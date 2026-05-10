package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.models.tasks.launchRoutes
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager


class RailsRouteAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        FileDocumentManager.getInstance().saveAllDocuments()
        launchRoutes(project)
    }
}