package com.github.thinkami.railroads.helper

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.ruby.rails.model.RailsApp
import org.jetbrains.plugins.ruby.ruby.codeInsight.resolve.scope.RElementWithFQN
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyPsiUtil
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.modules.RModule
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer
import org.jetbrains.plugins.ruby.ruby.lang.psi.indexes.RubyClassModuleNameIndex
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall
import org.jetbrains.plugins.ruby.utils.NamingConventions

class PsiUtil {
    companion object {
        private val INCLUDE_MODULE_FILTER = PsiElementFilter {
            (it is RCall) && (it as RCall).command == "include"
        }

        fun getControllerClassNameByShortName(shortName: String): String {
            return getControllerClassPathByShortName(shortName).joinToString("::")
        }

        fun findControllerClass(app: RailsApp, qualifiedClassName: String): RClass? {
            if (qualifiedClassName.isBlank()) {
                return null
            }

            val controller = app.findController(qualifiedClassName)
            if (controller != null) {
                return controller.rClass
            }

            val container = findClassOrModule(qualifiedClassName, app.project)

            return if (container is RClass) container else null
        }

        fun findClassOrModule(qualifiedName: String, project: Project) : RContainer? {
            val classPath = qualifiedName.split("::")
            val className = classPath[classPath.size - 1]

            val items = findClassesAndModules(className, project)

            for (item in items) {
                val name: String = item.fqn.fullPath

                if (qualifiedName.equals(name, ignoreCase = true)) {
                    return item as RContainer
                }
            }

            return null
        }

        fun findControllerMethod(app: RailsApp, controllerClass: RClass, methodName: String): RMethod? {
            var currentClass = controllerClass

            while (true) {
                var method = RubyPsiUtil.getMethodWithPossibleZeroArgsByName(currentClass, methodName)
                if (method != null) {
                    return method
                }

                method = findMethodInClassModules(currentClass, methodName)
                if (method != null) {
                    return method
                }

                // Search in parent classes (railways)
                val psiParentRef = currentClass.psiSuperClass
                if ((psiParentRef == null) || (psiParentRef.name == null)) {
                    return null
                }

                currentClass = findControllerClass(app, psiParentRef.name!!) ?: return null
            }
        }

        private fun getControllerClassPathByShortName(shortName: String): List<String> {
            val classPath = (shortName + "_controller").split("/")
            return classPath.map {
                NamingConventions.toCamelCase(it)
            }
        }

        private fun findClassesAndModules(name: String, project: Project): Collection<RElementWithFQN> {
            val scope = GlobalSearchScope.allScope(project)

            return StubIndex.getElements(RubyClassModuleNameIndex.KEY, name, project, scope, RElementWithFQN::class.java)
        }

        private fun findMethodInClassModules(controllerClass: RClass, methodName: String): RMethod? {
            val elements = PsiTreeUtil.collectElements(controllerClass, INCLUDE_MODULE_FILTER)

            // Iterate from the end of the list as next included module can override
            // same-named methods of previously included module.
            // (from railways)
            var i = elements.size

            while (--i >= 0) {
                val includeMethodCall = elements[i] as RCall
                val moduleNameArg = includeMethodCall.arguments[0] ?: continue
                val container = findClassOrModule(moduleNameArg.text, controllerClass.project)

                if (container is RModule) {
                    return RubyPsiUtil.getMethodWithPossibleZeroArgsByName(container, methodName)
                }
            }

            return null
        }
    }
}