package com.github.thinkami.railroads.models

import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import java.io.File
import java.io.FileInputStream

class RoutesTableFilterTest: BasePlatformTestCase() {
    private lateinit var model: RoutesTableModel

    override fun setUp() {
        super.setUp()

        val basePath = System.getProperty("user.dir")
        val inputStream = FileInputStream(File(basePath, "src/test/testData/RoutesTableFilterTest.data.txt"))
        val parser = RailsRoutesParser(module)
        val routes = parser.parse(inputStream)

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
}