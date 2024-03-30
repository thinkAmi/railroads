package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.models.tasks.RoutesTask
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class RailsRouteAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val task = RoutesTask(project)

        // Queueに積む場合
        task.queue()
        // 即時実行する場合
//        ProgressManager.getInstance().run(task)

    }
}