package com.sforca.callmanager.a11y

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Fails when user-visible text is hardcoded in production Kotlin instead of coming from string
 * resources, so literals don't creep back in after the string extraction. Everything from the first
 * `@Preview` in a file onwards is skipped: previews sit at the bottom by convention and use sample
 * text that is never shipped.
 */
class HardcodedStringsTest {
    private val sourceRoot = File("src/main/java")

    private val literalPatterns =
        listOf(
            Regex("""\bText\(\s*(text\s*=\s*)?""""),
            Regex("""contentDescription\s*=\s*""""),
            Regex("""\.setContent(Title|Text)\(""""),
        )

    @Test
    fun `production code has no hardcoded user-facing text`() {
        assertTrue("run from the :app module", sourceRoot.isDirectory)
        val offenders =
            sourceRoot
                .walkTopDown()
                .filter { it.extension == "kt" }
                .flatMap { file -> offendingLines(file).map { "${file.relativeTo(sourceRoot)}:$it" } }
                .toList()
        assertTrue(
            "Hardcoded UI text, move it to res/values/strings_*.xml:\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `scanner flags a literal in shipped code and skips previews`() {
        val shipped = """Text("Hello")"""
        assertTrue(literalPatterns.any { it.containsMatchIn(shipped) })
        assertTrue(literalPatterns.any { it.containsMatchIn("""Icon(x, contentDescription = "Back")""") })
        assertTrue(literalPatterns.none { it.containsMatchIn("Text(stringResource(R.string.x))") })
    }

    private fun offendingLines(file: File): List<Int> {
        val lines = file.readLines()
        val end = lines.indexOfFirst { it.trimStart().startsWith("@Preview") }.let { if (it < 0) lines.size else it }
        return lines
            .take(end)
            .withIndex()
            .filter { (_, line) -> literalPatterns.any { it.containsMatchIn(line) } }
            .map { it.index + 1 }
    }
}
