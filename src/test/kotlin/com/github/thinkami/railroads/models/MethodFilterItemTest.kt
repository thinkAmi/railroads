package com.github.thinkami.railroads.models

import junit.framework.TestCase

class MethodFilterItemTest : TestCase() {
    private fun item(method: String): MethodFilterItem {
        return MethodFilterItem(method, method)
    }

    fun testLabelIsUsedAsDisplayText() {
        TestCase.assertEquals("All", MethodFilterItem.ALL.toString())
        TestCase.assertEquals("(blank)", MethodFilterItem.BLANK.toString())
        TestCase.assertEquals("GET", item("GET").toString())
    }

    fun testNoRoutes() {
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL),
            buildMethodFilterItems(listOf(), null)
        )
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL),
            buildMethodFilterItems(listOf(), MethodFilterItem.ALL)
        )
    }

    fun testRoutesWithRequestMethods() {
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), item("POST")),
            buildMethodFilterItems(listOf("POST", "GET"), MethodFilterItem.ALL)
        )
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), item("POST")),
            buildMethodFilterItems(listOf("POST", "GET", "GET"), item("GET"))
        )
    }

    fun testRoutesWithBlankRequestMethod() {
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), item("POST"), MethodFilterItem.BLANK),
            buildMethodFilterItems(listOf("", "POST", "GET"), MethodFilterItem.ALL)
        )
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), MethodFilterItem.BLANK),
            buildMethodFilterItems(listOf("GET", ""), MethodFilterItem.BLANK)
        )
    }

    fun testSelectedRequestMethodNotInRoutes() {
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), item("POST"), item("DELETE")),
            buildMethodFilterItems(listOf("GET", "POST"), item("DELETE"))
        )

        // The selected request method is placed among the request methods, not after (blank)
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), item("DELETE"), item("OPTIONS"), MethodFilterItem.BLANK),
            buildMethodFilterItems(listOf("", "OPTIONS", "GET"), item("DELETE"))
        )
    }

    fun testSelectedBlankNotInRoutes() {
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, item("GET"), item("POST"), MethodFilterItem.BLANK),
            buildMethodFilterItems(listOf("GET", "POST"), MethodFilterItem.BLANK)
        )
        TestCase.assertEquals(
            listOf(MethodFilterItem.ALL, MethodFilterItem.BLANK),
            buildMethodFilterItems(listOf(), MethodFilterItem.BLANK)
        )
    }

    fun testOrderOfRequestMethods() {
        TestCase.assertEquals(
            listOf(
                MethodFilterItem.ALL,
                item("GET"),
                item("POST"),
                item("PUT"),
                item("PATCH"),
                item("DELETE"),
                item("HEAD"),
                item("OPTIONS"),
                item("TRACE"),
                MethodFilterItem.BLANK
            ),
            buildMethodFilterItems(
                listOf("TRACE", "", "DELETE", "OPTIONS", "PATCH", "GET", "HEAD", "PUT", "POST"),
                MethodFilterItem.ALL
            )
        )
    }
}
