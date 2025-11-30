package com.github.thinkami.railroads.workflow

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress

interface ProgressRunner {
    suspend fun <T> run(project: Project, title: String, action: suspend () -> T): T

    companion object {
        fun withBackgroundProgressRunner(): ProgressRunner = object : ProgressRunner {
            override suspend fun <T> run(project: Project, title: String, action: suspend () -> T): T =
                withBackgroundProgress(project, title) { action() }
        }
    }
}
