package com.github.thinkami.railroads.parser

import com.github.thinkami.railroads.models.MountedRailsEngine
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.models.routes.MountedEngineRoute
import com.github.thinkami.railroads.models.routes.RedirectRoute
import com.github.thinkami.railroads.models.routes.SimpleRoute
import com.intellij.openapi.module.Module
import io.ktor.utils.io.errors.*
import java.io.DataInputStream
import java.io.InputStream
import java.util.regex.Matcher
import java.util.regex.Pattern

class RailsRoutesParser(private val module: Module) {
    private val ROUTE_LINE = Pattern.compile("^([a-z0-9_]+)?\\s*([A-Z|]+)?\\s+([/(]\\S*?)\\s+(.+?)$")
    private val REQUIREMENTS = Pattern.compile("(\\{.+?}\\s*$)")
    private val REDIRECT = Pattern.compile("redirect\\(\\d+(?:,\\s*(.+?))?\\).*")
    private val ENGINE_ROUTES_HEADER_LINE = Pattern.compile("^Routes for ([a-zA-Z0-9:_]+):")
    private val MOUNTED_ENGINE_LINE = Pattern.compile("^\\s*([a-z0-9_]+)?\\s*([/(]\\S*?)\\s+([a-zA-Z0-9_]+)::Engine$")
    private val MOUNTED_ENGINE_CONTROLLER = Pattern.compile("([A-Z_][A-Za-z0-9_:/]+)")

    private val mountedEngines: MutableList<MountedRailsEngine> = mutableListOf()
    private var currentMountedRailsEngine: MountedRailsEngine? = null

    fun parse(stdOut: String): MutableList<BaseRoute> {
        return parse(stdOut.byteInputStream())
    }

    fun parse(stream: InputStream): MutableList<BaseRoute> {
        val result = mutableListOf<BaseRoute>()

        try {
            // https://stackoverflow.com/questions/41000584/best-way-to-use-bufferedreader-in-kotlin
            DataInputStream(stream).bufferedReader().forEachLine {
                val r = parseLine(it)
                if (r.size > 0) {
                    result.addAll(r)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return result
    }

    fun parseLine(line: String): MutableList<BaseRoute> {
        if (hasIgnorableValues(line)) {
            return mutableListOf()
        }

        val isMountedEngineLine = addMountedEnginesIfNeeded(line)
        if (isMountedEngineLine) {
            return mutableListOf()
        }

        val engineHeaderGroup = ENGINE_ROUTES_HEADER_LINE.matcher(line.trim())
        if (engineHeaderGroup.matches()) {
            val engineNameSpaceWithSuffix = getGroup(engineHeaderGroup, 1)
            currentMountedRailsEngine = findMountedEngine(engineNameSpaceWithSuffix)
            return mutableListOf()
        }

        val groups: Matcher = ROUTE_LINE.matcher(line.trim())
        if (!groups.matches()) {
            return mutableListOf()
        }

        val routeName = getGroup(groups, 1)
        var routePath = getGroup(groups, 3)
        val conditions = getGroup(groups, 4)
        val action = conditions.split("#", limit = 2)
        var routeController: String = ""
        var routeAction: String = ""
        var redirectPath: String = ""

        val redirectMatcher = REDIRECT.matcher(conditions)
        if (redirectMatcher.matches()) {
            redirectPath = getGroup(redirectMatcher, 1)
        } else if (action.size == 2) {
            // Process new format of output: 'controller#action'
            routeController = action[0]
            routeAction = extractRouteRequirements(action[1])
        }

        if (currentMountedRailsEngine != null) {
            routePath = if (routePath == "/") {
                currentMountedRailsEngine!!.rootPath
            } else {
                currentMountedRailsEngine!!.rootPath + routePath
            }
        }

        // For the following routes, multiple HTTP methods are set on a single line.
        // match '/multiple_match', to: 'multiple#call', via: [:get, :post]
        // Note: `|` cannot be split if escaped (`\\|`)
        val requestMethods = getGroup(groups, 2).split("|")
        val results = requestMethods.map {
            if (routeController.isBlank() && routeAction.isBlank()) {
                val mountedEngineController = captureFirstCroup(MOUNTED_ENGINE_CONTROLLER, conditions)
                if (mountedEngineController.isNotBlank()) {
                    MountedEngineRoute(module, it, routePath, routeName, mountedEngineController)
                }
            }

            if (redirectPath.isNotBlank()) {
                RedirectRoute(module, it, routePath, routeName, redirectPath)
            } else {
                SimpleRoute(module, it, routePath, routeName, routeController, routeAction)
            }
        }

        return results.toMutableList()
    }

    private fun hasIgnorableValues(line: String): Boolean {
        return line.contains("bin/bundle exec")
    }

    private fun addMountedEnginesIfNeeded(line: String): Boolean {
        val matcher = MOUNTED_ENGINE_LINE.matcher(line)
        if (matcher.matches()) {
            val engineName = getGroup(matcher, 1)
            val engineRootPath = getGroup(matcher, 2)
            val engineNameSpace = getGroup(matcher, 3)
            mountedEngines.add(MountedRailsEngine(
                engineName = engineName,
                rootPath = engineRootPath,
                nameSpace = engineNameSpace
            ))
            return true
        }

        return false
    }

    private fun findMountedEngine(engineNameSpaceWithSuffix: String): MountedRailsEngine? {
        val engineNameSpace = engineNameSpaceWithSuffix.split("::")[0]
       return mountedEngines.find { it.nameSpace == engineNameSpace }
    }

    private fun getGroup(matcher: Matcher, groupIndex: Int): String {
        return matcher.group(groupIndex)?.trim() ?: ""
    }

    private fun extractRouteRequirements(actionWithOption: String): String {
        val requirements = captureFirstCroup(REQUIREMENTS, actionWithOption)

        return actionWithOption.substring(0, actionWithOption.length - requirements.length).trim()
    }

    private fun captureFirstCroup(pattern: Pattern, subject: String): String {
        val m = pattern.matcher(subject)
        return if (m.find()) m.group() else ""
    }
}