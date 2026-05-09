package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.ui.RailroadIcon
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class SimpleRouteSnapshotTest : BasePlatformTestCase() {

    fun testSnapshotIsImmutableAfterConstruction() {
        // RailsApp.fromModule(module) returns null in BasePlatformTestCase, so railsAction.psiClass / psiMethod
        // stay null. The current implementation falls back to PsiUtil.getControllerClassNameByShortName(controllerName),
        // which means a later mutation of `controllerName` (a public `var` on BaseRoute) changes the live return value.
        // The snapshot implementation must capture the value at init time and not be affected by later mutations.
        val route = SimpleRoute(
            myFixture.module,
            "GET",
            "/users",
            "users",
            "users",
            "index"
        )

        val before = route.getQualifiedActionTitle()
        // Fallback for "users" -> "UsersController" + "#index"
        TestCase.assertEquals("UsersController#index", before)

        // Mutate the open `var controllerName` on BaseRoute. In the current (live) implementation,
        // getQualifiedActionTitle() will recompute via the fallback path with the new name and return a
        // different value ("Admin::UsersController#index"). In the snapshot implementation, the return value
        // must stay equal to `before` because the value was frozen at init time.
        route.controllerName = "admin/users"

        val after = route.getQualifiedActionTitle()
        TestCase.assertEquals(before, after)
    }

    fun testResolveActionIconReturnsMethodProvidedIconWhenMethodIsPresent() {
        // When hasMethod=true, the method-icon provider's result wins regardless of class presence.
        val expected = RailroadIcon.NodeRouteAction
        val icon = SimpleRoute.resolveActionIcon(
            hasMethod = true,
            hasClass = true,
            methodIconProvider = { expected }
        )
        TestCase.assertSame(expected, icon)
    }

    fun testResolveActionIconFallsBackToControllerIconWhenOnlyClassIsPresent() {
        // hasMethod=false, hasClass=true -> NodeController, and the method-icon provider must NOT be invoked.
        var providerInvoked = false
        val icon = SimpleRoute.resolveActionIcon(
            hasMethod = false,
            hasClass = true,
            methodIconProvider = {
                providerInvoked = true
                RailroadIcon.NodeRouteAction
            }
        )
        TestCase.assertSame(RailroadIcon.NodeController, icon)
        TestCase.assertFalse("method icon provider must not be invoked when hasMethod=false", providerInvoked)
    }

    fun testResolveActionIconFallsBackToUnknownWhenNothingResolved() {
        // hasMethod=false, hasClass=false -> Unknown, and the method-icon provider must NOT be invoked.
        var providerInvoked = false
        val icon = SimpleRoute.resolveActionIcon(
            hasMethod = false,
            hasClass = false,
            methodIconProvider = {
                providerInvoked = true
                RailroadIcon.NodeRouteAction
            }
        )
        TestCase.assertSame(RailroadIcon.Unknown, icon)
        TestCase.assertFalse("method icon provider must not be invoked when hasMethod=false", providerInvoked)
    }

    fun testFreshlyConstructedRouteWithoutRailsAppHasUnresolvedDefaults() {
        // RailsApp.fromModule returns null in BasePlatformTestCase, so railsAction.psiClass / psiMethod
        // remain null, and the snapshot must reflect the fully-unresolved state.
        val route = SimpleRoute(
            myFixture.module,
            "GET",
            "/users",
            "users",
            "users",
            "index"
        )

        TestCase.assertSame(RailroadIcon.Unknown, route.getActionIcon())
        TestCase.assertFalse(route.canNavigate())
        TestCase.assertFalse(route.methodExists())
    }

    fun testDynamicRouteSkipsPsiResolution() {
        // When controllerName contains a colon (e.g. ":controller#:action"), the qualified action
        // title must equal the action title - the qualified path's PSI resolution is skipped because
        // the controller is a parameter, not a real class name.
        val route = SimpleRoute(
            myFixture.module,
            "GET",
            "/:controller/:action",
            "",
            ":controller",
            ":action"
        )

        TestCase.assertEquals(":controller#:action", route.getActionTitle())
        TestCase.assertEquals(route.getActionTitle(), route.getQualifiedActionTitle())
    }

    fun testNestedControllerFallback() {
        // When PSI is unresolved (test fixture has no Rails app), the qualified action title falls
        // back to PsiUtil.getControllerClassNameByShortName which converts slash-separated
        // controller names into "::"-separated FQNs.
        val route = SimpleRoute(
            myFixture.module,
            "GET",
            "/admin/users",
            "admin_users",
            "admin/users",
            "index"
        )

        TestCase.assertEquals("admin/users#index", route.getActionTitle())
        TestCase.assertEquals("Admin::UsersController#index", route.getQualifiedActionTitle())
    }
}
