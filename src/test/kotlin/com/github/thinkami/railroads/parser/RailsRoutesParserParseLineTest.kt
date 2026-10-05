package com.github.thinkami.railroads.parser

import com.github.thinkami.railroads.models.routes.BaseRoute
import com.github.thinkami.railroads.models.routes.RedirectRoute
import com.github.thinkami.railroads.models.routes.SimpleRoute
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import kotlin.reflect.KClass

class RailsRoutesParserParseLineTest: BasePlatformTestCase() {
    fun testNamedGetRoute() {
        assertParsedLine(
            "    blog_post_comments GET      /blogs/:blog_id/posts/:post_id/comments(.:format)   blogs/posts/comments#index",
            SimpleRoute::class,
            "blog_post_comments",
            "GET",
            "/blogs/:blog_id/posts/:post_id/comments(.:format)",
            "blogs/posts/comments#index")
    }

    fun testUnnamedPutRoute() {
        assertParsedLine(
            "  PUT      /blogs/:blog_id/posts/:post_id/comments/:id(.:format)   blogs/posts/comments#update",
            SimpleRoute::class,
            "",
            "PUT",
            "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
            "blogs/posts/comments#update")
    }

    fun testUnnamedPatchRoute() {
        assertParsedLine(
            "  PATCH    /blogs/:blog_id/posts/:post_id/comments/:id(.:format)    blogs/posts/comments#update",
            SimpleRoute::class,
            "",
            "PATCH",
            "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
            "blogs/posts/comments#update")
    }

    fun testUnnamedDeleteRoute() {
        assertParsedLine(
            "   DELETE   /blogs/:blog_id/posts/:post_id/comments/:id(.:format)   blogs/posts/comments#destroy",
            SimpleRoute::class,
            "",
            "DELETE",
            "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
            "blogs/posts/comments#destroy")
    }

    fun testRackAppRoute() {
        assertParsedLine(
            "         rack_app    /rack_app(.:format)      #<HelloRackApp:0x000001988fad1b40>",
            SimpleRoute::class,
            "rack_app",
            "",
            "/rack_app(.:format)",
            "<HelloRackApp:0x000001988fad1b40>")
    }

    fun testInlineHandlerRoute() {
        assertParsedLine(
            "        inline GET      /inline(.:format)       Inline handler (Proc/Lambda)",
            SimpleRoute::class,
            "inline",
            "GET",
            "/inline(.:format)",
            "")
    }

    fun testRouteWithRequirements() {
        assertParsedLine(
            "  GET      /photos/:id(.:format)      photos#show {:id=>/[A-Z]\\d{5}/}",
            SimpleRoute::class,
            "",
            "GET",
            "/photos/:id(.:format)",
            "photos#show")
    }

    fun testRedirectRoute() {
        assertParsedLine(
            "  redirect GET      /redirect(.:format)         redirect(301, /blogs)",
            RedirectRoute::class,
            "redirect",
            "GET",
            "/redirect(.:format)",
            "/blogs")
    }

    private fun assertParsedLine(
        line: String,
        routeClass: KClass<out BaseRoute>,
        routeName: String,
        method: String,
        path: String,
        actionTitle: String,
    ) {
        val parsedLine = RailsRoutesParser(module).parseLine(line)

        TestCase.assertEquals(1, parsedLine.size)

        val actual = parsedLine.first()
        TestCase.assertEquals(routeClass, actual::class)
        TestCase.assertEquals(routeName, actual.routeName)
        TestCase.assertEquals(method, actual.requestMethod)
        TestCase.assertEquals(path, actual.routePath)
        TestCase.assertEquals(actionTitle, actual.getActionTitle())
    }
}
