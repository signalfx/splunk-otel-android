import java.io.File

object Ktlint {

    // Directory containing source files that should be checked by ktlint
    const val INPUT_DIR = "src"

    // Directory relative to `buildDir` used to export detailed ktlint reports
    const val OUTPUT_DIR = "/reports/ktlint/"

    // Wildcard defining all allowed source files
    const val INCLUDED_FILES = "**/*.kt"

    fun outputDir(buildDir: File) = "$buildDir$OUTPUT_DIR"
}
