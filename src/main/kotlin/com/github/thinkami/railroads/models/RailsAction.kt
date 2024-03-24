package com.github.thinkami.railroads.models

import com.github.thinkami.railroads.helper.PsiUtil
import com.intellij.openapi.module.Module
import org.jetbrains.plugins.ruby.rails.model.RailsApp
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod

class RailsAction {
    var psiClass: RClass? = null
    var psiMethod: RMethod? = null


    fun update(module: Module, controllerName: String, actionName: String) {
        val app = RailsApp.fromModule(module)
        update(app, controllerName, actionName)
    }

    fun update(app: RailsApp?, controllerShortName: String, actionName: String) {
        if ((app == null) || controllerShortName.isBlank()) {
            psiClass = null
            psiMethod = null
            return
        }

        val qualifiedName = PsiUtil.getControllerClassNameByShortName(controllerShortName)

        if (psiClass != null && (!psiClass!!.isValid || psiClass!!.fqn.fullPath != qualifiedName)) {
            psiClass = null
            psiMethod = null
        }

        if (psiClass == null) {
            psiClass = PsiUtil.findControllerClass(app, qualifiedName)
        }

        if (psiClass != null) {
            if (psiMethod == null || !psiMethod!!.isValid) {
                psiMethod = PsiUtil.findControllerMethod(app, psiClass!!, actionName)
            }
        } else {
            // (railways)
            // Even if psiMethod is valid, its name can be different - it
            // usually happens when user edits method name - the psiElement
            // is just updated.
            if (psiMethod != null && actionName != psiMethod!!.name) {
                psiMethod = null
            }
        }
    }
}