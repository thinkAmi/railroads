package com.github.thinkami.railroads.models.routes

import com.github.thinkami.railroads.helper.PsiUtil
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer
import java.io.File

/**
 * Resolves the navigation target of each route in src/test/testData/navigation/routes.txt
 * against the Ruby files in src/test/testData/navigation/project.
 *
 * RailsApp.fromModule() returns null in BasePlatformTestCase, so the controller lookup of the
 * Rails support is replaced with [recordingFinder], which records the requested names and finds
 * nothing. The rest of the lookup runs against real Ruby PSI.
 */
class RouteNavigationTargetTest : BasePlatformTestCase() {
    private val requestedControllerNames = mutableListOf<String>()
    private val recordingFinder: (String) -> RClass? = { name ->
        requestedControllerNames.add(name)
        null
    }

    override fun getTestDataPath(): String = "${System.getProperty("user.dir")}/src/test/testData/navigation"

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("project", "")
    }

    fun testAction() {
        assertTarget("users#index", "UsersController", "UsersController#index")
    }

    fun testActionInheritedFromSuperclass() {
        assertTarget("users#inherited_action", "UsersController", "ApplicationController#inherited_action")
    }

    fun testActionDefinedInIncludedModule() {
        assertTarget("users#search", "UsersController", "Searchable#search")
    }

    fun testMissingActionStillResolvesController() {
        assertTarget("users#missing", "UsersController", null)
    }

    fun testActionOfControllerNestedInModule() {
        assertTarget("admin/reports#show", "Admin::ReportsController", "Admin::ReportsController#show")
    }

    fun testActionOfControllerDefinedWithCompactName() {
        assertTarget("admin/settings#edit", "Admin::SettingsController", "Admin::SettingsController#edit")
    }

    fun testActionOfControllerDefinedWithCompactNameInsideModule() {
        assertTarget(
            "rails/conductor/action_mailbox/inbound_emails#index",
            "Rails::Conductor::ActionMailbox::InboundEmailsController",
            "Rails::Conductor::ActionMailbox::InboundEmailsController#index"
        )
    }

    fun testActionOfControllerWithAcronymNamespaceWithoutRailsSupport() {
        // The route gives api/users, while the class is API::UsersController. Only the Rails support
        // knows the acronym, so the name built without it does not match.
        assertTarget("api/users#index", null, null)
    }

    fun testActionOfControllerFoundByRailsSupport() {
        // Stand-in for the Rails support with `inflect.acronym 'API'`.
        val railsSupport: (String) -> RClass? = { name ->
            if (name == "api/users") PsiUtil.findClassOrModule("API::UsersController", project) as? RClass else null
        }

        val (controllerClass, method) = resolve("api/users#index", railsSupport)

        TestCase.assertEquals("API::UsersController", controllerClass?.fqnWithNesting?.fullPath)
        TestCase.assertEquals("API::UsersController#index", describe(method))
    }

    fun testUnknownController() {
        assertTarget("ghosts#index", null, null)
    }

    fun testControllerLookupReceivesRoutePathOnly() {
        // The superclass is looked up by its class name, not through the controller lookup.
        resolve("users#inherited_action")

        TestCase.assertEquals(listOf("users"), requestedControllerNames)
    }

    fun testMountedEngineClass() {
        val target = MountedEngineRoute.resolveNavigationTarget("Blorgh::Engine", project)

        TestCase.assertNotNull(target)
        TestCase.assertEquals("engine.rb", target!!.first.name)
    }

    fun testUnknownMountedEngineClass() {
        TestCase.assertNull(MountedEngineRoute.resolveNavigationTarget("Ghost::Engine", project))
    }

    private fun assertTarget(controllerAction: String, expectedClass: String?, expectedMethod: String?) {
        val (controllerClass, method) = resolve(controllerAction)

        TestCase.assertEquals("class of $controllerAction", expectedClass, controllerClass?.fqnWithNesting?.fullPath)
        TestCase.assertEquals("method of $controllerAction", expectedMethod, describe(method))
    }

    private fun describe(method: RMethod?): String? = method?.let {
        val owner = PsiTreeUtil.getParentOfType(it, RContainer::class.java)
        "${owner?.fqnWithNesting?.fullPath}#${it.methodName?.name}"
    }

    private fun resolve(
        controllerAction: String,
        findController: (String) -> RClass? = recordingFinder
    ) = findRoute(controllerAction).let { route ->
        val controllerClass = PsiUtil.findControllerClassForRoute(project, route.controllerName, findController)
        val method = controllerClass?.let { PsiUtil.findControllerMethod(project, it, route.actionName) }
        Pair(controllerClass, method)
    }

    private fun findRoute(controllerAction: String): BaseRoute {
        val routes = RailsRoutesParser(module).parse(File(testDataPath, "routes.txt").readText())
        return routes.single { "${it.controllerName}#${it.actionName}" == controllerAction }
    }
}
