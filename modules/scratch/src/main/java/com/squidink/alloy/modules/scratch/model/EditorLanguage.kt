package com.squidink.alloy.modules.scratch.model

import dev.snipme.highlights.model.SyntaxLanguage

/**
 * Enum representing supported editor languages with associated metadata.
 *
 * @property displayName The human-readable name of the language.
 * @property extension The file extension associated with the language.
 * @property syntaxLanguage The syntax highlighting language definition, or null if not applicable.
 * @property mimeType The MIME type for storage export.
 */
enum class EditorLanguage(
    val displayName: String,
    val extension: String,
    val syntaxLanguage: SyntaxLanguage?,
    val mimeType: String = "text/plain"
) {
    PLAIN_TEXT("Plain Text", "txt", null, "text/plain"),
    MARKDOWN("Markdown", "md", null, "text/markdown"),
    KOTLIN("Kotlin", "kt", SyntaxLanguage.KOTLIN, "text/x-kotlin"),
    JAVA("Java", "java", SyntaxLanguage.JAVA, "text/x-java-source"),
    PYTHON("Python", "py", SyntaxLanguage.PYTHON, "text/x-python"),
    RUST("Rust", "rs", SyntaxLanguage.RUST, "text/x-rust"),
    CPP("C++", "cpp", SyntaxLanguage.CPP, "text/x-c++src"),
    C("C", "c", SyntaxLanguage.C, "text/x-csrc"),
    CSHARP("C#", "cs", SyntaxLanguage.CSHARP, "text/x-csharp"),
    GO("Go", "go", SyntaxLanguage.GO, "text/x-go"),
    JAVASCRIPT("JavaScript", "js", SyntaxLanguage.JAVASCRIPT, "text/javascript"),
    TYPESCRIPT("TypeScript", "ts", SyntaxLanguage.TYPESCRIPT, "text/typescript"),
    SHELL("Shell / Bash", "sh", SyntaxLanguage.SHELL, "text/x-sh"),
    SWIFT("Swift", "swift", SyntaxLanguage.SWIFT, "text/x-swift"),
    PHP("PHP", "php", SyntaxLanguage.PHP, "text/x-php"),
    RUBY("Ruby", "rb", SyntaxLanguage.RUBY, "text/x-ruby"),
    DART("Dart", "dart", SyntaxLanguage.DART, "text/x-dart");

    companion object {
        /**
         * Finds the [EditorLanguage] by its file extension.
         *
         * @param ext The file extension string (e.g., "kt", "java").
         * @return The matching [EditorLanguage], or [PLAIN_TEXT] if no match is found.
         */
        fun fromExtension(ext: String): EditorLanguage {
            return entries.find { it.extension.equals(ext, ignoreCase = true) }
                ?: PLAIN_TEXT
        }

        /**
         * Finds the [EditorLanguage] by its name or display name.
         *
         * @param name The language name (e.g., "Kotlin", "KOTLIN").
         * @return The matching [EditorLanguage], or [PLAIN_TEXT] if no match is found.
         */
        fun fromName(name: String): EditorLanguage {
            return entries.find {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            } ?: PLAIN_TEXT
        }
    }
}
