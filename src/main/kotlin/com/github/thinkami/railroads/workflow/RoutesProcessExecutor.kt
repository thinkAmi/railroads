package com.github.thinkami.railroads.workflow

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.parser.RailsRoutesParser
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext

data class ProcessResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val wasCancelled: Boolean,
    val routes: List<BaseRoute> = emptyList()
)

interface RoutesProcessExecutor {
    suspend fun run(project: Project): ProcessResult
}

class RoutesProcessExecutorImpl(
    private val railsContextProvider: RailsContextProvider = RailsContextProvider.Default(),
    private val commandRunner: RoutesCommandRunner = RubyRoutesCommandRunner(),
    private val logger: Logger = Logger.getInstance(RoutesProcessExecutorImpl::class.java)
) : RoutesProcessExecutor {
    override suspend fun run(project: Project): ProcessResult {
        val context = try {
            railsContextProvider.getContext(project)
        } catch (t: Throwable) {
            logger.warn("Failed to resolve rails context", t)
            null
        }
        if (context == null) {
            return ProcessResult(
                stdout = "",
                stderr = "Rails context not found (module/app/sdk)",
                exitCode = 1,
                wasCancelled = false
            )
        }
        val output = withContext(Dispatchers.IO) {
            commandRunner.run(context)
        }
        val routes = withContext(Dispatchers.Default) {
            RailsRoutesParser(context.module).parse(output.stdout)
        }
        return ProcessResult(
            stdout = output.stdout,
            stderr = output.stderr,
            exitCode = output.exitCode,
            wasCancelled = output.isCancelled,
            routes = routes
        )
    }
}

interface RoutesCommandRunner {
    suspend fun run(context: RailsContext): ProcessOutput
}

class RubyRoutesCommandRunner : RoutesCommandRunner {
    override suspend fun run(context: RailsContext): ProcessOutput {
        var result: ProcessOutput = ProcessOutput()
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                result = RubyGemExecutionContext
                    .create(context.sdk, "rails")
                    .withModule(context.module)
                    .withWorkingDirPath(context.appRoot)
                    .withArguments("routes", "--trace")
                    .executeScript() ?: ProcessOutput()
            },
            "Running rails routes",
            true,
            context.module.project
        )
        return result
    }
}
