package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.module.Module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class MountedEngineRouteTest: DescribeSpec ({
    describe("getActionIcon") {
        val module = mockk<Module>()
        val actual = MountedEngineRoute(
            module,
            "GET",
            "/test",
            "",
            "Test::Engine"
        )

        it("icon is NodeMountedEngine ") {
            actual.getActionIcon().shouldBe(RailroadIcon.NodeMountedEngine)
        }
    }
})