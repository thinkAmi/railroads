package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MountedEngineRouteTest: BasePlatformTestCase() {
    fun testGetActionIcon() {
        val actual = MountedEngineRoute(
            module,
            "GET",
            "/test",
            "",
            "Test::Engine"
        )
        assertEquals(RailroadIcon.NodeMountedEngine, actual.getActionIcon())
    }
}