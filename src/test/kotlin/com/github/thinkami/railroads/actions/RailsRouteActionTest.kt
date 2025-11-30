package com.github.thinkami.railroads.actions

import com.github.thinkami.railroads.workflow.RoutesRunner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestActionEvent

class RailsRouteActionTest : LightPlatformTestCase() {

    private class RecordingRunner : RoutesRunner {
        var called = false
        var project: com.intellij.openapi.project.Project? = null
        override fun requestRoutes(project: com.intellij.openapi.project.Project) {
            called = true
            this.project = project
        }
    }

    fun testDoesNothingWhenProjectIsNull() {
        val runner = RecordingRunner()
        val action = RailsRouteAction { null }
        val dataContext = SimpleDataContext.builder().build()
        val event = TestActionEvent.createTestEvent(createDummyAction(), dataContext)

        action.actionPerformed(event)

        assertTrue(!runner.called)
    }

    fun testInvokesRunnerWhenProjectIsPresent() {
        val runner = RecordingRunner()
        val action = RailsRouteAction { runner }
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .build()
        val event = TestActionEvent.createTestEvent(createDummyAction(), dataContext)

        action.actionPerformed(event)

        assertTrue(runner.called)
        assertSame(project, runner.project)
    }

    private fun createDummyAction(): AnAction = object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {}
    }
}
