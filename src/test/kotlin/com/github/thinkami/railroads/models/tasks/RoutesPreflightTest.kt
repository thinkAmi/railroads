package com.github.thinkami.railroads.models.tasks

import junit.framework.TestCase

class RoutesPreflightTest : TestCase() {
    fun testNoModule() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.NoModule,
            decideRoutesPreflight(moduleCount = 0, railsApplicationCount = 0, hasSdk = false)
        )
        TestCase.assertEquals(
            RoutesPreflightResultKind.NoModule,
            decideRoutesPreflight(moduleCount = 0, railsApplicationCount = 1, hasSdk = true)
        )
    }

    fun testNotRailsApplication() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.NotRailsApplication,
            decideRoutesPreflight(moduleCount = 1, railsApplicationCount = 0, hasSdk = false)
        )
        TestCase.assertEquals(
            RoutesPreflightResultKind.NotRailsApplication,
            decideRoutesPreflight(moduleCount = 3, railsApplicationCount = 0, hasSdk = true)
        )
    }

    fun testMultipleRailsApplications() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.MultipleRailsApplications,
            decideRoutesPreflight(moduleCount = 2, railsApplicationCount = 2, hasSdk = true)
        )
        TestCase.assertEquals(
            RoutesPreflightResultKind.MultipleRailsApplications,
            decideRoutesPreflight(moduleCount = 3, railsApplicationCount = 2, hasSdk = false)
        )
    }

    fun testMissingRubySdk() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.MissingRubySdk,
            decideRoutesPreflight(moduleCount = 2, railsApplicationCount = 1, hasSdk = false)
        )
    }

    fun testReady() {
        TestCase.assertEquals(
            RoutesPreflightResultKind.Ready,
            decideRoutesPreflight(moduleCount = 2, railsApplicationCount = 1, hasSdk = true)
        )
    }
}
