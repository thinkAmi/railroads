package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.concurrency.AppExecutorUtil

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

    // Regression guard: calling resolveNavigationTarget without a read action must throw.
    // assertReadAccessAllowed() always returns true on the EDT, so we dispatch the call to
    // a background thread via AppExecutorUtil. The captured-Throwable pattern (instead of
    // try { ... fail() } catch (Throwable)) avoids fail()'s AssertionError being swallowed
    // by the catch block.
    //
    // We deliberately don't add happy-path tests that actually invoke the helper inside a
    // read action: those would require Ruby plugin services (BundleConfigService) and the
    // Ruby StubIndex extension ("Ruby.class.module.shortName"), neither of which is
    // initialized by BasePlatformTestCase. Bringing those services up here is a fixture cost
    // we explicitly want to avoid, so the happy-path behavior is covered by code review
    // against SimpleRoute.navigate() and by manual sandbox verification instead.
    fun testResolveNavigationTargetThrowsWhenCalledOutsideReadAction() {
        var caught: Throwable? = null
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                MountedEngineRoute.resolveNavigationTarget("Test::Engine", project)
            } catch (t: Throwable) {
                caught = t
            }
        }.get()

        val captured = caught
        assertNotNull(
            "resolveNavigationTarget must throw when called without a read action",
            captured
        )

        // Verify the exception is the read access assertion, not a downstream Ruby
        // StubIndex / fixture failure. Without this, removing assertReadAccessAllowed()
        // from the helper would still pass the test because PsiUtil.findClassOrModule()
        // also throws in BasePlatformTestCase (Ruby plugin services / StubIndex extensions
        // are not initialized in the test fixture).
        val combinedMessage = generateSequence(captured) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" | ")
        assertTrue(
            "expected read-access assertion failure, got: ${captured?.javaClass?.name}: $combinedMessage",
            combinedMessage.contains("Read access", ignoreCase = true) ||
                combinedMessage.contains("read action", ignoreCase = true) ||
                combinedMessage.contains("read-action", ignoreCase = true)
        )
    }
}
