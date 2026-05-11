package com.github.thinkami.railroads.providers

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class RouteCopyProviderTest : BasePlatformTestCase() {
    fun testActionUpdateThreadIsEdt() {
        TestCase.assertEquals(ActionUpdateThread.EDT, RouteCopyProvider(null).actionUpdateThread)
    }
}
