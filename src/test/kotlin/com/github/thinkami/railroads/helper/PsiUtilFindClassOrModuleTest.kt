package com.github.thinkami.railroads.helper

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.modules.RModule
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer

/**
 * Pins down how PsiUtil.findClassOrModule() resolves qualified names against real Ruby PSI.
 * The Ruby files are copied from src/test/testData/navigation/project.
 */
class PsiUtilFindClassOrModuleTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "${System.getProperty("user.dir")}/src/test/testData/navigation"

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("project", "")
    }

    fun testFindsTopLevelClass() {
        assertFound("UsersController", RClass::class.java, "UsersController")
    }

    fun testFindsClassNestedInModule() {
        assertFound("Admin::ReportsController", RClass::class.java, "Admin::ReportsController")
    }

    fun testFindsClassDefinedWithCompactName() {
        assertFound("Admin::SettingsController", RClass::class.java, "Admin::SettingsController")
    }

    fun testFindsClassDefinedWithCompactNameInsideModule() {
        // module Rails
        //   class Conductor::ActionMailbox::InboundEmailsController
        assertFound(
            "Rails::Conductor::ActionMailbox::InboundEmailsController",
            RClass::class.java,
            "Rails::Conductor::ActionMailbox::InboundEmailsController"
        )
    }

    fun testFindsModule() {
        assertFound("Searchable", RModule::class.java, "Searchable")
    }

    fun testFindsEngineClass() {
        assertFound("Blorgh::Engine", RClass::class.java, "Blorgh::Engine")
    }

    fun testDistinguishesClassesWithTheSameShortName() {
        // Both UsersController and API::UsersController exist.
        assertFound("API::UsersController", RClass::class.java, "API::UsersController")
    }

    fun testFindsClassWhoseNameDiffersOnlyInCase() {
        assertFound("Api::UsersController", RClass::class.java, "API::UsersController")
    }

    fun testReturnsNullForClassInAnotherNamespace() {
        assertNotFound("Other::UsersController")
    }

    fun testReturnsNullForConstantThatIsNotClassOrModule() {
        // Blorgh::Version is assigned with Struct.new, not defined with the class keyword.
        assertNotFound("Blorgh::Version")
    }

    fun testReturnsNullForUnknownName() {
        assertNotFound("GhostsController")
    }

    private fun assertFound(qualifiedName: String, expectedType: Class<out RContainer>, expectedFqn: String) {
        val actual = PsiUtil.findClassOrModule(qualifiedName, project)

        TestCase.assertNotNull("$qualifiedName should be found", actual)
        TestCase.assertTrue(
            "$qualifiedName should be ${expectedType.simpleName}, but was ${actual!!.javaClass.simpleName}",
            expectedType.isInstance(actual)
        )
        TestCase.assertEquals(expectedFqn, actual.fqnWithNesting.fullPath)
    }

    private fun assertNotFound(qualifiedName: String) {
        TestCase.assertNull("$qualifiedName should not be found", PsiUtil.findClassOrModule(qualifiedName, project))
    }
}
