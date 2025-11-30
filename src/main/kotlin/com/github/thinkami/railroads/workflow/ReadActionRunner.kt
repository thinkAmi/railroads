package com.github.thinkami.railroads.workflow

import com.intellij.openapi.application.readAction

interface ReadActionRunner {
    suspend fun <T> run(block: () -> T): T

    companion object {
        fun default(): ReadActionRunner = object : ReadActionRunner {
            override suspend fun <T> run(block: () -> T): T = readAction { block() }
        }
    }
}
