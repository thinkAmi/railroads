package com.github.thinkami.railroads.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class CopyRouteActionBaseTest : BasePlatformTestCase() {
    fun testCopyRouteNameActionUpdatesOnEdt() {
        TestCase.assertEquals(ActionUpdateThread.EDT, CopyRouteNameAction().actionUpdateThread)
    }

    fun testCopyRoutePathActionUpdatesOnEdt() {
        TestCase.assertEquals(ActionUpdateThread.EDT, CopyRoutePathAction().actionUpdateThread)
    }

    fun testCopyRouteFullPathActionUpdatesOnEdt() {
        TestCase.assertEquals(ActionUpdateThread.EDT, CopyRouteFullPathAction().actionUpdateThread)
    }
}
