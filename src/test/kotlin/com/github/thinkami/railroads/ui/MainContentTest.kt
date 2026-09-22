package com.github.thinkami.railroads.ui

import com.github.thinkami.railroads.models.RoutesTableModel
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import com.intellij.ui.table.JBTable
import junit.framework.TestCase
import java.io.File
import java.io.FileInputStream
import javax.swing.JComboBox
import javax.swing.JLabel

// Component tests that operate the Swing components of MainContent directly.
// Components are looked up by type and name, not by their position in the layout.
class MainContentTest: BasePlatformTestCase() {
    private lateinit var mainContent: MainContent
    private lateinit var routes: List<BaseRoute>

    override fun setUp() {
        super.setUp()

        mainContent = MainContent()

        val basePath = System.getProperty("user.dir")
        val inputStream = FileInputStream(File(basePath, "src/test/testData/RoutesTableFilterTest.data.txt"))
        routes = RailsRoutesParser(module).parse(inputStream)
    }

    fun testCreateMainContent() {
        TestCase.assertNotNull(mainContent.content)
    }

    fun testMethodFilterIsDirectChildOfContent() {
        // MainView only looks up the direct children of the DialogPanel when disabling the header menu
        val comboBoxes = mainContent.content.components.filterIsInstance<JComboBox<*>>().filter {
            it.name == "methodFilter"
        }

        TestCase.assertEquals(1, comboBoxes.size)
    }

    fun testInitialMethodFilterItems() {
        TestCase.assertEquals(listOf("All"), methodFilterLabels())
        TestCase.assertEquals("All", methodFilter().selectedItem.toString())
    }

    fun testMethodFilterItemsAfterRoutesLoaded() {
        loadRoutes(routes)

        TestCase.assertEquals(listOf("All", "GET", "POST", "PUT", "PATCH", "DELETE", "(blank)"), methodFilterLabels())
        TestCase.assertEquals("All", methodFilter().selectedItem.toString())
        TestCase.assertEquals("69/69", routesCounter().text)
    }

    fun testSelectGetMethod() {
        loadRoutes(routes)
        selectMethod("GET")

        TestCase.assertEquals(41, table().rowCount)
        TestCase.assertEquals("41/69", routesCounter().text)
    }

    fun testMethodFilterWithSearchText() {
        loadRoutes(routes)

        searchTextField().text = "shops"
        TestCase.assertEquals(2, table().rowCount)
        TestCase.assertEquals("2/69", routesCounter().text)

        selectMethod("POST")
        TestCase.assertEquals(1, table().rowCount)
        TestCase.assertEquals("1/69", routesCounter().text)
    }

    fun testSelectedMethodIsKeptAfterReloadingRoutes() {
        loadRoutes(routes)
        selectMethod("DELETE")
        TestCase.assertEquals("3/69", routesCounter().text)

        loadRoutes(routes.filter { it.requestMethod != "DELETE" })

        TestCase.assertEquals("DELETE", methodFilter().selectedItem.toString())
        TestCase.assertEquals(0, table().rowCount)
        TestCase.assertEquals("0/66", routesCounter().text)
        TestCase.assertEquals(listOf("All", "GET", "POST", "PUT", "PATCH", "DELETE", "(blank)"), methodFilterLabels())
    }

    private fun methodFilter(): JComboBox<*> {
        return mainContent.content.components.filterIsInstance<JComboBox<*>>().first {
            it.name == "methodFilter"
        }
    }

    private fun methodFilterLabels(): List<String> {
        val comboBox = methodFilter()
        return (0 until comboBox.itemCount).map { comboBox.getItemAt(it).toString() }
    }

    private fun selectMethod(label: String) {
        val comboBox = methodFilter()
        comboBox.selectedItem = (0 until comboBox.itemCount).map { comboBox.getItemAt(it) }.first {
            it.toString() == label
        }
    }

    private fun searchTextField(): SearchTextField {
        return mainContent.content.components.filterIsInstance<SearchTextField>().first()
    }

    private fun routesCounter(): JLabel {
        return mainContent.content.components.filterIsInstance<JLabel>().first {
            it.name == "routesCounter"
        }
    }

    private fun table(): JBTable {
        val scrollPane = mainContent.content.components.filterIsInstance<JBScrollPane>().first()
        val viewport = scrollPane.components.filterIsInstance<JBViewport>().first {
            it.components.filterIsInstance<JBTable>().isNotEmpty()
        }
        return viewport.components.filterIsInstance<JBTable>().first()
    }

    private fun loadRoutes(routes: List<BaseRoute>) {
        (table().model as RoutesTableModel).updateTableDataFromRoutes(routes)
    }
}
