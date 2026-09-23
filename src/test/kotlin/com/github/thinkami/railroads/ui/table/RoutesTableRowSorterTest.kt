package com.github.thinkami.railroads.ui.table

import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import java.io.File
import java.io.FileInputStream
import javax.swing.RowSorter
import javax.swing.SortOrder

// Unit tests of the row sorter without a JTable.
// Column indexes: 0 = Method, 1 = Path, 2 = Action, 3 = Name
class RoutesTableRowSorterTest: BasePlatformTestCase() {
    private lateinit var model: RoutesTableModel
    private lateinit var sorter: RoutesTableRowSorter
    private lateinit var routes: List<BaseRoute>

    override fun setUp() {
        super.setUp()

        val basePath = System.getProperty("user.dir")
        val inputStream = FileInputStream(File(basePath, "src/test/testData/RoutesTableFilterTest.data.txt"))
        routes = RailsRoutesParser(module).parse(inputStream)

        model = RoutesTableModel()
        sorter = RoutesTableRowSorter(model)

        // A JTable notifies its row sorter of model changes. Do the same here because there is no JTable.
        model.addTableModelListener { sorter.allRowsChanged() }

        model.updateTableDataFromRoutes(routes)
    }

    fun testUnsortedByDefault() {
        TestCase.assertEquals(listOf<RowSorter.SortKey>(), sorter.sortKeys)
        TestCase.assertEquals(routes, viewRoutes())
    }

    fun testSortByMethodAscending() {
        sortBy(0, SortOrder.ASCENDING)

        TestCase.assertEquals(
            List(41) { "GET" } + List(17) { "POST" } + List(4) { "PUT" } + List(3) { "PATCH" } + List(3) { "DELETE" } + listOf(""),
            viewRoutes().map { it.requestMethod }
        )

        // Routes with the same request method keep the order of rails routes
        TestCase.assertEquals(routes.filter { it.requestMethod == "GET" }, viewRoutes().take(41))
    }

    fun testSortByMethodDescending() {
        sortBy(0, SortOrder.DESCENDING)

        TestCase.assertEquals(
            listOf("") + List(3) { "DELETE" } + List(3) { "PATCH" } + List(4) { "PUT" } + List(17) { "POST" } + List(41) { "GET" },
            viewRoutes().map { it.requestMethod }
        )

        // Routes with the same request method keep the order of rails routes
        TestCase.assertEquals(routes.filter { it.requestMethod == "DELETE" }, viewRoutes().subList(1, 4))
    }

    fun testRequestMethodComparator() {
        TestCase.assertEquals(
            listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", ""),
            listOf("TRACE", "", "DELETE", "OPTIONS", "PATCH", "GET", "HEAD", "PUT", "POST")
                .sortedWith(RoutesTableRowSorter.REQUEST_METHOD_COMPARATOR)
        )
    }

    fun testSortByPathAscending() {
        sortBy(1, SortOrder.ASCENDING)
        val paths = viewRoutes().map { it.routePath }

        TestCase.assertEquals("/", paths.first())
        TestCase.assertEquals("/videos/unknown(.:format)", paths.last())

        // A parent path comes before its child paths
        TestCase.assertEquals(
            listOf(
                "/blogs(.:format)",
                "/blogs/:blog_id/forum/categories(.:format)",
                "/blogs/:blog_id/posts(.:format)",
                "/blogs/:blog_id/posts/:id(.:format)",
                "/blogs/:blog_id/posts/:id/edit(.:format)",
                "/blogs/:blog_id/posts/:post_id/comments(.:format)",
                "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
                "/blogs/:blog_id/posts/:post_id/comments/:id/edit(.:format)",
                "/blogs/:blog_id/posts/:post_id/comments/new(.:format)",
                "/blogs/:blog_id/posts/new(.:format)",
                "/blogs/:id(.:format)",
                "/blogs/:id/edit(.:format)",
                "/blogs/new(.:format)",
            ),
            paths.filter { it.startsWith("/blogs") }.distinct()
        )
    }

    fun testSortByPathDescending() {
        sortBy(1, SortOrder.ASCENDING)
        val ascendingPaths = viewRoutes().map { it.routePath }

        sortBy(1, SortOrder.DESCENDING)

        TestCase.assertEquals(ascendingPaths.reversed(), viewRoutes().map { it.routePath })
    }

    fun testTextComparator() {
        TestCase.assertEquals(
            listOf("/articles", "/Blogs(.:format)", "/blogs/new", ""),
            listOf("", "/blogs/new", "/Blogs(.:format)", "/articles").sortedWith(RoutesTableRowSorter.TEXT_COMPARATOR)
        )

        TestCase.assertEquals(0, RoutesTableRowSorter.TEXT_COMPARATOR.compare("/Articles", "/articles"))
    }

    fun testSortByActionAscending() {
        sortBy(2, SortOrder.ASCENDING)
        val titles = viewRoutes().map { it.getActionTitle() }

        TestCase.assertEquals(listOf("/", "/blogs", "<HelloRackApp:0x000001988fad1b40>"), titles.take(3))
        TestCase.assertEquals(titles.dropLast(1).sortedWith(String.CASE_INSENSITIVE_ORDER), titles.dropLast(1))

        // expected route
        // inline GET /inline(.:format) Inline handler (Proc/Lambda)
        TestCase.assertEquals("", titles.last())
        TestCase.assertEquals("inline", viewRoutes().last().routeName)
    }

    fun testSortByActionDescending() {
        sortBy(2, SortOrder.DESCENDING)
        val titles = viewRoutes().map { it.getActionTitle() }

        TestCase.assertEquals("", titles.first())
        TestCase.assertEquals("/", titles.last())
    }

    fun testSortByNameAscending() {
        sortBy(3, SortOrder.ASCENDING)
        val view = viewRoutes()

        TestCase.assertEquals("articles", view.first().routeName)
        TestCase.assertEquals(
            listOf("blog", "blog_forum_categories", "blog_post", "blog_post_comment", "blog_post_comments", "blog_posts", "blogs"),
            view.map { it.routeName }.filter { it.startsWith("blog") }
        )

        // Routes without a name come last, in the order of rails routes
        TestCase.assertEquals(routes.filter { it.routeName.isEmpty() }, view.takeLast(20))
    }

    fun testSortByNameDescending() {
        sortBy(3, SortOrder.DESCENDING)
        val view = viewRoutes()

        // Routes without a name come first, in the order of rails routes
        TestCase.assertEquals(routes.filter { it.routeName.isEmpty() }, view.take(20))

        TestCase.assertEquals("videos_unknown", view[20].routeName)
        TestCase.assertEquals("articles", view.last().routeName)
    }

    fun testToggleSortOrderCyclesThroughAscendingDescendingAndUnsorted() {
        sorter.toggleSortOrder(1)
        TestCase.assertEquals(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING)), sorter.sortKeys)

        sorter.toggleSortOrder(1)
        TestCase.assertEquals(listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)), sorter.sortKeys)

        sorter.toggleSortOrder(1)
        TestCase.assertEquals(listOf<RowSorter.SortKey>(), sorter.sortKeys)
        TestCase.assertEquals(routes, viewRoutes())

        sorter.toggleSortOrder(1)
        TestCase.assertEquals(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING)), sorter.sortKeys)
    }

    fun testToggleAnotherColumnSortsOnlyByThatColumn() {
        sorter.toggleSortOrder(0)
        sorter.toggleSortOrder(0)
        sorter.toggleSortOrder(3)

        TestCase.assertEquals(listOf(RowSorter.SortKey(3, SortOrder.ASCENDING)), sorter.sortKeys)

        // Routes without a name are not sorted by the Method column
        TestCase.assertEquals(routes.filter { it.routeName.isEmpty() }, viewRoutes().takeLast(20))

        // A descending column goes back to ascending when it is sorted again after another column
        sorter.toggleSortOrder(0)
        TestCase.assertEquals(listOf(RowSorter.SortKey(0, SortOrder.ASCENDING)), sorter.sortKeys)
    }

    fun testSortIsKeptAfterFilterChanges() {
        sortBy(1, SortOrder.DESCENDING)

        model.tableFilter.requestMethod = "POST"

        TestCase.assertEquals(listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)), sorter.sortKeys)
        val paths = viewRoutes().map { it.routePath }
        TestCase.assertEquals(17, paths.size)
        TestCase.assertEquals("/shops(.:format)", paths.first())
        TestCase.assertEquals("/blogs(.:format)", paths.last())

        model.tableFilter.filterText = "blogs"

        TestCase.assertEquals(
            listOf(
                "/blogs/:blog_id/posts/:post_id/comments(.:format)",
                "/blogs/:blog_id/posts(.:format)",
                "/blogs(.:format)",
            ),
            viewRoutes().map { it.routePath }
        )
    }

    fun testSortIsKeptAfterRoutesUpdated() {
        sortBy(0, SortOrder.ASCENDING)

        model.updateTableDataFromRoutes(routes.filter { it.requestMethod != "GET" })

        TestCase.assertEquals(listOf(RowSorter.SortKey(0, SortOrder.ASCENDING)), sorter.sortKeys)
        TestCase.assertEquals(
            List(17) { "POST" } + List(4) { "PUT" } + List(3) { "PATCH" } + List(3) { "DELETE" } + listOf(""),
            viewRoutes().map { it.requestMethod }
        )
    }

    fun testConvertRowIndexBetweenViewAndModel() {
        val rackAppModelIndex = routes.indexOfFirst { it.routeName == "rack_app" }

        sortBy(0, SortOrder.DESCENDING)
        TestCase.assertEquals(0, sorter.convertRowIndexToView(rackAppModelIndex))
        TestCase.assertEquals(rackAppModelIndex, sorter.convertRowIndexToModel(0))

        sortBy(0, SortOrder.ASCENDING)
        TestCase.assertEquals(68, sorter.convertRowIndexToView(rackAppModelIndex))
        TestCase.assertEquals(rackAppModelIndex, sorter.convertRowIndexToModel(68))
    }

    private fun sortBy(column: Int, sortOrder: SortOrder) {
        sorter.sortKeys = listOf(RowSorter.SortKey(column, sortOrder))
    }

    private fun viewRoutes(): List<BaseRoute> {
        return (0 until sorter.viewRowCount).map { model.getRoute(sorter.convertRowIndexToModel(it)) }
    }
}
