package com.yangchengwei.easytrip

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioMatrixReferenceTest {
    @Test
    fun everyMarkdownClassMethodReferenceResolvesToAndroidTestSource() {
        val root = findRepositoryRoot()
        val matrix = root.resolve("docs/testing/v1-full-ui-scenario-matrix.md").toFile().readText()
        val references = Regex("`([A-Za-z_][\\w.]*)#([A-Za-z_]\\w*)`")
            .findAll(matrix)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toSet()
        val discovered = discoverAndroidTestMethods(root.resolve("app/src/androidTest/java"))
        val missing = references.filterNot { it in discovered }

        println("scenario matrix references=${references.size} missing=${missing.size}")
        assertTrue("Missing scenario matrix test references: $missing", missing.isEmpty())
        assertTrue("Matrix must label paths as declared or expected metadata", matrix.contains("声明路径/预期路径"))
        assertFalse("Matrix must not claim catalog paths prove reachability", matrix.contains("| Reachable path |"))
        assertFalse("Matrix must not relabel declared metadata as executed navigation", matrix.contains("| 已执行导航 |"))
        val productionDeletionPath = "实际 production E2E：旅行列表 → 继续规划 → 工作台更多 → 设置 → 删除这次旅行 → 确认 → 旅行列表"
        assertTrue(
            "d1sTtb and oW9mK must distinguish their declared design path from the actual production E2E",
            matrix.split(productionDeletionPath).size - 1 == 2,
        )
    }

    private fun findRepositoryRoot(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (current != null) {
            if (current.resolve("docs/testing/v1-full-ui-scenario-matrix.md").isRegularFile() &&
                current.resolve("app/src/androidTest/java").isDirectory()
            ) return current
            current = current.parent
        }
        error("Could not locate Easy Trip repository root from ${System.getProperty("user.dir")}")
    }

    private fun discoverAndroidTestMethods(sourceRoot: Path): Set<Pair<String, String>> {
        val classPattern = Regex("(?:class|interface|object)\\s+([A-Za-z_]\\w*)")
        val testMethodPattern = Regex("@Test(?:\\([^)]*\\))?\\s*(?:\\r?\\n\\s*)*fun\\s+([A-Za-z_]\\w*)")
        val result = mutableSetOf<Pair<String, String>>()

        Files.walk(sourceRoot).use { paths ->
            paths.filter { it.isRegularFile() && it.name.endsWith(".kt") }.forEach { file ->
                val text = file.toFile().readText()
                classPattern.findAll(text).forEach { declaration ->
                    val bodyStart = text.indexOf('{', declaration.range.last + 1)
                    if (bodyStart < 0) return@forEach
                    val bodyEnd = matchingBrace(text, bodyStart)
                    val className = declaration.groupValues[1]
                    testMethodPattern.findAll(text.substring(bodyStart + 1, bodyEnd)).forEach { method ->
                        result += className to method.groupValues[1]
                    }
                }
            }
        }
        return result
    }

    private fun matchingBrace(source: String, openingIndex: Int): Int {
        var depth = 0
        source.substring(openingIndex).forEachIndexed { offset, character ->
            when (character) {
                '{' -> depth++
                '}' -> if (--depth == 0) return openingIndex + offset
            }
        }
        error("Unclosed class body in scenario matrix test source")
    }
}
