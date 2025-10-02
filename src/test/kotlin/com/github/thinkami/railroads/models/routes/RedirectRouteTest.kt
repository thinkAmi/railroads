package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.testFramework.fixtures.BasePlatformTestCase


class RedirectRouteTest: BasePlatformTestCase() {
    fun testGetActionIconWithRedirectPath() {
        val actual = RedirectRoute(
            module,
            "GET",
            "/test",
            "redirect",
            "/test_redirect"
        )

        assertEquals(RailroadIcon.NodeRedirect, actual.getActionIcon())
        assertEquals("/test_redirect", actual.getActionTitle())
        assertEquals("redirect to /test_redirect", actual.getQualifiedActionTitle())
    }

    fun testGetActionIconWithNullRedirectPath() {
        val actual = RedirectRoute(
            module,
            "GET",
            "/test",
            "redirect",
            null
        )

        assertEquals(RailroadIcon.NodeRedirect, actual.getActionIcon())
        assertEquals("[redirect]", actual.getActionTitle())
        assertEquals("[runtime define redirect]", actual.getQualifiedActionTitle())
    }
}