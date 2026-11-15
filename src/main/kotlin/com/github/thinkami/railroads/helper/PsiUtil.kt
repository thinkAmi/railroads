package com.github.thinkami.railroads.helper

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.ruby.rails.facet.RailsApp
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyFQNUtil
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.TypeSet
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.fqn.FQN
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyPsiUtil
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.modules.RModule
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall
import org.jetbrains.plugins.ruby.utils.NamingConventions

class PsiUtil {
    companion object {
        private val INCLUDE_MODULE_FILTER = PsiElementFilter {
            (it is RCall) && (it as RCall).command == "include"
        }

        private val CLASS_MODULE_TYPES = TypeSet(Type.CLASS, Type.MODULE)

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
            if (controllerShortName.isBlank()) {
                return null
            }

            // The Rails support takes the controller path of the route and applies the inflection
            // rules of the application, such as acronyms, when it builds the class name.
            val controller = findController(controllerShortName)
            if (controller != null) {
                return controller
            }

            val qualifiedName = getControllerClassNameByShortName(controllerShortName)
            return findClassOrModule(qualifiedName, project) as? RClass
        }

        fun findClassOrModule(qualifiedName: String, project: Project) : RContainer? {
            if (qualifiedName.isBlank()) {
                return null
            }

            return RubyFQNUtil.findContainerByFQN(project, CLASS_MODULE_TYPES, FQN.of(qualifiedName), null)
        }

        fun findControllerMethod(project: Project, controllerClass: RClass, methodName: String): RMethod? {
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

                currentClass = findClassOrModule(psiParentRef.name!!, project) as? RClass ?: return null
            }
        }

        private fun getControllerClassPathByShortName(shortName: String): List<String> {
            val classPath = (shortName + "_controller").split("/")
            return classPath.map {
                NamingConventions.toCamelCase(it)
            }
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