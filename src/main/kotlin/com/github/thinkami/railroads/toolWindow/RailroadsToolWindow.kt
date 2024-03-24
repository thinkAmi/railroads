package com.github.thinkami.railroads.toolWindow

import com.github.thinkami.railroads.ui.MainContent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RailroadsToolWindow: ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(MainContent().content, "", false)
        toolWindow.contentManager.addContent(content)
    }
}