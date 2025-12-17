package com.github.thinkami.railroads.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class RoutesCoroutineService(
    val project: Project,
    // CoroutineScope tied to the service lifecycle is injected from the IntelliJ Platform
    val scope: CoroutineScope,
)
