package com.github.thinkami.railroads.helper

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.ruby.rails.facet.RailsApp
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

        /**
         * Returns a controller lookup backed by the Rails support of the Ruby plugin.
         */
        fun railsControllerFinder(app: RailsApp): (String) -> RClass? = { name ->
            app.findController(name)?.rClass
        }

        /**
         * Finds the controller class of a route, such as `admin/users` of `admin/users#index`.
         *
         * [findController] is the controller lookup of the Rails support. It is a parameter so that
         * tests can run without a Rails application, where `RailsApp.fromModule()` returns null.
         */
        fun findControllerClassForRoute(
            project: Project,
            controllerShortName: String,
            findController: (String) -> RClass?
        ): RClass? {
            val qualifiedName = getControllerClassNameByShortName(controllerShortName)
            return findControllerClass(project, qualifiedName, findController)
        }

        fun findControllerClass(
            project: Project,
            qualifiedClassName: String,
            findController: (String) -> RClass?
        ): RClass? {
            if (qualifiedClassName.isBlank()) {
                return null
            }

            val controller = findController(qualifiedClassName)
            if (controller != null) {
                return controller
            }

            val container = findClassOrModule(qualifiedClassName, project)

            return if (container is RClass) container else null
        }

        fun findClassOrModule(qualifiedName: String, project: Project) : RContainer? {
            val classPath = qualifiedName.split("::")
            val className = classPath[classPath.size - 1]

            val items = findClassesAndModules(className, project)

            for (item in items) {
                // `fqn.fullPath` causes Rails routing to fail, so use `fqnWithNesting.fullPath` instead.
                // Rails routing example: /rails/conductor/action_mailbox/inbound_emails
                val name = item.fqnWithNesting.fullPath

                // The StubIndex can return non-RContainer elements such as RConstantImpl
                // (e.g. when `include SomeConstant` references a non-module constant).
                // Skip them to avoid ClassCastException.
                if (qualifiedName.equals(name, ignoreCase = true) && item is RContainer) {
                    return item
                }
            }

            return null
        }

        fun findControllerMethod(
            project: Project,
            controllerClass: RClass,
            methodName: String,
            findController: (String) -> RClass?
        ): RMethod? {
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

                currentClass = findControllerClass(project, psiParentRef.name!!, findController) ?: return null
            }
        }

        private fun getControllerClassPathByShortName(shortName: String): List<String> {
            val classPath = (shortName + "_controller").split("/")
            return classPath.map {
                NamingConventions.toCamelCase(it)
            }
        }

        private fun findClassesAndModules(name: String, project: Project): Collection<RElementWithFQN> {
            // Since IDE 2025.2, the `RubyProjectAndLibrariesScope` have been no longer available, so we should use the equivalent `GlobalSearchScope.allScope` instead.
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