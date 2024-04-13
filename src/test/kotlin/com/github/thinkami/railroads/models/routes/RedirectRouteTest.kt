package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.openapi.module.Module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class RedirectRouteTest: DescribeSpec({
    describe("RedirectRoute") {
        context ("set value at redirect route path") {
            // RedirectRoute does not use modules, so mock module
            val module = mockk<Module>()
            val actual = RedirectRoute(
                module,
                "GET",
                "/test",
                "redirect",
                "/test_redirect"
            )

            it("the title includes path") {
                actual.getActionIcon().shouldBe(RailroadIcon.NodeRedirect)
                actual.getActionTitle().shouldBe("/test_redirect")
                actual.getQualifiedActionTitle().shouldBe("redirect to /test_redirect")
            }
        }

        context ("set null at redirect route path") {
            // RedirectRoute does not use modules, so mock module
            val module = mockk<Module>()
            val actual = RedirectRoute(
                module,
                "GET",
                "/test",
                "redirect",
                null
            )

            it("the title is a fixed value") {
                actual.getActionIcon().shouldBe(RailroadIcon.NodeRedirect)
                actual.getActionTitle().shouldBe("[redirect]")
                actual.getQualifiedActionTitle().shouldBe("[runtime define redirect]")
            }
        }
    }
})