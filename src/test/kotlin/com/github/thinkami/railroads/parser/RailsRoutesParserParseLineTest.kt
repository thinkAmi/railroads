package com.github.thinkami.railroads.parser

import com.github.thinkami.railroads.models.routes.RedirectRoute
import com.github.thinkami.railroads.models.routes.SimpleRoute
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class RailsRoutesParserParseLineTest: BasePlatformTestCase() {
    // The fact that BasePlatformTestCase predates JUnit5 may have an impact.
    //
    // If there is no test method and only JUnit5's parameterized test is used,
    // the test will fail with the following error.
    // junit.framework.AssertionFailedError: No tests found in com.github.thinkami.railroads.parser.RailsRoutesParserParseLineTest
    fun testDummy() {
        TestCase.assertEquals(1, 1)
    }

    @ParameterizedTest
    @MethodSource("variousRoute")
    fun testParseVariousLine(line: String, routeClass: String, routeName: String, method: String, path: String, actionTitle: String) {
        // The parameterized test is written in JUnit5, but the BasePlatformTestCase is implemented in a format earlier than JUnit5.
        // If nothing is done, the setUp method of BasePlatformTestCase is not executed and the module is not set in the fixture.
        // Therefore, by calling the setUp method, the module is set.
        setUp()

        val parser = RailsRoutesParser(module)
        val parsedLine = parser.parseLine(line)

        TestCase.assertNotNull(parsedLine)
        TestCase.assertEquals(1, parsedLine.size)

        val actual = parsedLine.first()
        TestCase.assertEquals(routeName, actual.routeName)
        TestCase.assertEquals(routeClass, actual::class.simpleName)
        TestCase.assertEquals(method, actual.requestMethod)
        TestCase.assertEquals(path, actual.routePath)
        TestCase.assertEquals(actionTitle, actual.getActionTitle())
    }

    companion object {
        @JvmStatic
        fun variousRoute(): Stream<Arguments> {
            return Stream.of(
                Arguments.arguments(
                    "    blog_post_comments GET      /blogs/:blog_id/posts/:post_id/comments(.:format)   blogs/posts/comments#index",
                    SimpleRoute::class.simpleName,
                    "blog_post_comments",
                    "GET",
                    "/blogs/:blog_id/posts/:post_id/comments(.:format)",
                    "blogs/posts/comments#index"),
                Arguments.arguments(
                    "  PUT      /blogs/:blog_id/posts/:post_id/comments/:id(.:format)   blogs/posts/comments#update",
                    SimpleRoute::class.simpleName,
                    "",
                    "PUT",
                    "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
                    "blogs/posts/comments#update"),
                Arguments.arguments(
                    "  PATCH    /blogs/:blog_id/posts/:post_id/comments/:id(.:format)    blogs/posts/comments#update",
                    SimpleRoute::class.simpleName,
                    "",
                    "PATCH",
                    "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
                    "blogs/posts/comments#update"),
                Arguments.arguments(
                    "   DELETE   /blogs/:blog_id/posts/:post_id/comments/:id(.:format)   blogs/posts/comments#destroy",
                    SimpleRoute::class.simpleName,
                    "",
                    "DELETE",
                    "/blogs/:blog_id/posts/:post_id/comments/:id(.:format)",
                    "blogs/posts/comments#destroy"),

                // route for Rack application
                Arguments.arguments(
                    "         rack_app    /rack_app(.:format)      #<HelloRackApp:0x000001988fad1b40>",
                    SimpleRoute::class.simpleName,
                    "rack_app",
                    "",
                    "/rack_app(.:format)",
                    "<HelloRackApp:0x000001988fad1b40>"),

                // inline handler
                Arguments.arguments(
                    "        inline GET      /inline(.:format)       Inline handler (Proc/Lambda)",
                    SimpleRoute::class.simpleName,
                    "inline",
                    "GET",
                    "/inline(.:format)",
                    ""),

                // route with additional requirements
                Arguments.arguments(
                    "  GET      /photos/:id(.:format)      photos#show {:id=>/[A-Z]\\d{5}/}",
                    SimpleRoute::class.simpleName,
                    "",
                    "GET",
                    "/photos/:id(.:format)",
                    "photos#show"),

                // redirect route
                Arguments.arguments(
                    "  redirect GET      /redirect(.:format)         redirect(301, /blogs)",
                    RedirectRoute::class.simpleName,
                    "redirect",
                    "GET",
                    "/redirect(.:format)",
                    "/blogs"),
            )
        }
    }
}
