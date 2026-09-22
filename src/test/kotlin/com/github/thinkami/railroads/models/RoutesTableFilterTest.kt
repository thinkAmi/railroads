package com.github.thinkami.railroads.models

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import java.io.File
import java.io.FileInputStream

class RoutesTableFilterTest: BasePlatformTestCase() {
    private lateinit var model: RoutesTableModel
    private lateinit var routes: List<BaseRoute>

    override fun setUp() {
        super.setUp()

        val basePath = System.getProperty("user.dir")
        val inputStream = FileInputStream(File(basePath, "src/test/testData/RoutesTableFilterTest.data.txt"))
        val parser = RailsRoutesParser(module)
        routes = parser.parse(inputStream)

        model = RoutesTableModel()
        model.updateTableDataFromRoutes(routes)
    }

    fun testSimplePathFilter() {
        val filter = model.tableFilter
        filter.filterText = "videos"

        TestCase.assertEquals(2, model.rowCount)
    }

    fun testWildcardPathFilter() {
        val filter = model.tableFilter
        filter.filterText = "blogs/*/edit"

        // expected route paths
        // /blogs/:blog_id/posts/:post_id/comments/:id/edit(.:format)
        // /blogs/:blog_id/posts/:id/edit(.:format)
        // /blogs/:id/edit(.:format)
        TestCase.assertEquals(3, model.rowCount)
    }

    fun testMethodFilter() {
        val filter = model.tableFilter
        filter.filterText = "#unknown"

        TestCase.assertEquals(1, model.rowCount)
    }

    fun testCaseInsensitivity() {
        val filter = model.tableFilter
        filter.filterText = "vIDeOS"

        TestCase.assertEquals(2, model.rowCount)
    }

    fun testRequestMethodFilter() {
        val filter = model.tableFilter

        filter.requestMethod = "GET"
        TestCase.assertEquals(41, model.rowCount)
        TestCase.assertEquals(69, model.getTotalRoutesCount())

        filter.requestMethod = "DELETE"
        TestCase.assertEquals(3, model.rowCount)

        filter.requestMethod = null
        TestCase.assertEquals(69, model.rowCount)
    }

    fun testRequestMethodAndTextFilter() {
        val filter = model.tableFilter

        // expected routes
        // GET  /shops(.:format) shops#index
        // POST /shops(.:format) shops#create
        filter.filterText = "shops"
        TestCase.assertEquals(2, model.rowCount)

        filter.requestMethod = "POST"
        TestCase.assertEquals(1, model.rowCount)
        TestCase.assertEquals("shops#create", model.getRoute(0).getActionTitle())
    }

    fun testBlankRequestMethodFilter() {
        val filter = model.tableFilter

        // expected route
        // rack_app /rack_app(.:format) #<HelloRackApp:0x000001988fad1b40>
        filter.requestMethod = ""
        TestCase.assertEquals(1, model.rowCount)
        TestCase.assertEquals("rack_app", model.getRoute(0).routeName)

        // All includes routes without a verb
        filter.requestMethod = null
        filter.filterText = "rack_app"
        TestCase.assertEquals(1, model.rowCount)
    }

    fun testNonexistentRequestMethodFilter() {
        val filter = model.tableFilter
        filter.requestMethod = "OPTIONS"

        TestCase.assertEquals(0, model.rowCount)
    }

    fun testRequestMethodFilterIsKeptAfterRoutesUpdated() {
        val filter = model.tableFilter
        filter.requestMethod = "DELETE"
        TestCase.assertEquals(3, model.rowCount)

        model.updateTableDataFromRoutes(routes.filter { it.requestMethod != "DELETE" })
        TestCase.assertEquals("DELETE", filter.requestMethod)
        TestCase.assertEquals(0, model.rowCount)
        TestCase.assertEquals(66, model.getTotalRoutesCount())

        model.updateTableDataFromRoutes(routes)
        TestCase.assertEquals("DELETE", filter.requestMethod)
        TestCase.assertEquals(3, model.rowCount)
    }

    fun testOnRoutesUpdatedIsCalledOnlyWhenRoutesAreReplaced() {
        var calledCount = 0
        model.onRoutesUpdated = { calledCount++ }

        val filter = model.tableFilter
        filter.filterText = "videos"
        filter.requestMethod = "GET"
        TestCase.assertEquals(0, calledCount)

        model.updateTableDataFromRoutes(routes)
        TestCase.assertEquals(1, calledCount)
    }

    fun testRequestMethods() {
        TestCase.assertEquals(
            listOf("", "GET", "POST", "PATCH", "PUT", "DELETE").sorted(),
            model.getRequestMethods().sorted()
        )
    }
}