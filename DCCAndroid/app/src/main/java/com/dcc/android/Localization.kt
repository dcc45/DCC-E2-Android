package com.dcc.android

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    ARABIC("ar"),
    URDU("ur")
}

fun text(en: String, ar: String, ur: String, language: AppLanguage): String = when (language) {
    AppLanguage.ENGLISH -> en
    AppLanguage.ARABIC -> ar
    AppLanguage.URDU -> ur
}
