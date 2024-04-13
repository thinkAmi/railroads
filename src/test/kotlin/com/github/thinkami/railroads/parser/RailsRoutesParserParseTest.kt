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
}
