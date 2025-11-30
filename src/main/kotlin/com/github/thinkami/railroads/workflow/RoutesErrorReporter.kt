package com.github.thinkami.railroads.workflow

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

interface RoutesErrorReporter {
    fun notify(project: Project, message: String, throwable: Throwable? = null)
}

class NotificationRoutesErrorReporter(
    private val logger: Logger = Logger.getInstance(NotificationRoutesErrorReporter::class.java)
) : RoutesErrorReporter {
    override fun notify(project: Project, message: String, throwable: Throwable?) {
        logger.warn(message, throwable)
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }

    private companion object {
        private const val NOTIFICATION_GROUP_ID = "railroadsNotification"
    }
}
