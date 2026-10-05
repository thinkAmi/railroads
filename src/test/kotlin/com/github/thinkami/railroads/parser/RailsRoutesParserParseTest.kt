package com.github.thinkami.railroads.parser

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import java.io.File
import java.io.FileInputStream

class RailsRoutesParserParseTest: BasePlatformTestCase() {
    fun testParse() {
        val basePath = System.getProperty("user.dir")
        val inputStream = FileInputStream(File(basePath, "src/test/testData/RailsRoutesParserTest.data.txt"))
        val parser = RailsRoutesParser(module)
        val actual = parser.parse(inputStream)

        // 8 routes and 1 multiple route
        TestCase.assertEquals(10, actual.size)
    }

    fun testParseCrlfOutput() {
        val basePath = System.getProperty("user.dir")
        val lfOutput = File(basePath, "src/test/testData/RailsRoutesParserTest.data.txt").readText()
        TestCase.assertFalse(lfOutput.contains('\r'))
        val crlfOutput = lfOutput.replace("\n", "\r\n")

        val expected = RailsRoutesParser(module).parse(lfOutput)
        val actual = RailsRoutesParser(module).parse(crlfOutput)

        TestCase.assertEquals(10, actual.size)
        TestCase.assertEquals(expected.map { it::class }, actual.map { it::class })
        TestCase.assertEquals(expected.map { it.requestMethod }, actual.map { it.requestMethod })
        TestCase.assertEquals(expected.map { it.routePath }, actual.map { it.routePath })
        TestCase.assertEquals(expected.map { it.routeName }, actual.map { it.routeName })
        TestCase.assertEquals(expected.map { it.getActionTitle() }, actual.map { it.getActionTitle() })
    }
}
