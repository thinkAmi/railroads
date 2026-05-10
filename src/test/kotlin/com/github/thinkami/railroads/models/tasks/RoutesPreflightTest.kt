package com.github.thinkami.railroads.models.tasks

import junit.framework.TestCase

class RoutesPreflightTest : TestCase() {
    fun testNoModule() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.NoModule,
            decideRoutesPreflight(hasModule = false, hasRailsRoot = false, hasSdk = false)
        )
        TestCase.assertEquals(
            RoutesPreflightResultKind.NoModule,
            decideRoutesPreflight(hasModule = false, hasRailsRoot = true, hasSdk = true)
        )
    }

    fun testNotRailsApplication() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.NotRailsApplication,
            decideRoutesPreflight(hasModule = true, hasRailsRoot = false, hasSdk = false)
        )
        TestCase.assertEquals(
            RoutesPreflightResultKind.NotRailsApplication,
            decideRoutesPreflight(hasModule = true, hasRailsRoot = false, hasSdk = true)
        )
    }

    fun testMissingRubySdk() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.MissingRubySdk,
            decideRoutesPreflight(hasModule = true, hasRailsRoot = true, hasSdk = false)
        )
    }

    fun testReady() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.Ready,
            decideRoutesPreflight(hasModule = true, hasRailsRoot = true, hasSdk = true)
        )
    }
}
