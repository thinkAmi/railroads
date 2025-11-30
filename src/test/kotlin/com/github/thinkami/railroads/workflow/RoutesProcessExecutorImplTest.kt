package com.github.thinkami.railroads.workflow

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.projectRoots.impl.UnknownSdkType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking

class RoutesProcessExecutorImplTest : LightPlatformTestCase() {

    private fun ensureSdk(): Sdk {
        val sdk = ModuleRootManager.getInstance(module).sdk
            ?: ProjectJdkTable.getInstance().allJdks.firstOrNull()
            ?: createUnknownSdk()
        return sdk
    }

    private fun createUnknownSdk(): Sdk {
        val table = ProjectJdkTable.getInstance()
        val sdk = ProjectJdkImpl(
            "test-sdk",
            UnknownSdkType.getInstance("test-sdk"),
            System.getProperty("java.home"),
            "1.0"
        )
        ApplicationManager.getApplication().runWriteAction {
            table.addJdk(sdk, testRootDisposable)
        }
        return sdk
    }

    fun testReturnsErrorWhenContextMissing() = runBlocking {
        val provider = object : RailsContextProvider {
            override suspend fun getContext(project: com.intellij.openapi.project.Project): RailsContext? = null
        }
        val runner = object : RoutesCommandRunner {
            var called = false
            override suspend fun run(context: RailsContext): ProcessOutput {
                called = true
                return ProcessOutput()
            }
        }
        val executor = RoutesProcessExecutorImpl(provider, runner)

        val result = executor.run(project)

        assertEquals(1, result.exitCode)
        assertTrue(result.stderr.contains("Rails context not found"))
        assertTrue(result.routes.isEmpty())
        assertFalse(result.wasCancelled)
        assertFalse(runner.called)
    }

    fun testExecutesCommandAndParsesRoutes() = runBlocking {
        val module = module
        val sdk = ensureSdk()
        var receivedContext: RailsContext? = null
        val provider = object : RailsContextProvider {
            override suspend fun getContext(project: com.intellij.openapi.project.Project): RailsContext =
                RailsContext(module, "/app", sdk)
        }
        val runner = object : RoutesCommandRunner {
            var called = false
            override suspend fun run(context: RailsContext): ProcessOutput {
                called = true
                receivedContext = context
                return ProcessOutput().apply {
                    appendStdout("users GET /users users#index")
                    exitCode = 0
                }
            }
        }
        val executor = RoutesProcessExecutorImpl(provider, runner)

        val result = executor.run(project)

        assertTrue(runner.called)
        assertSame(module, receivedContext?.module)
        assertEquals(0, result.exitCode)
        assertTrue(result.stderr.isEmpty())
        assertEquals(1, result.routes.size)
        val route = result.routes.first()
        assertEquals("/users", route.routePath)
        assertEquals("GET", route.requestMethod)
    }
}
