package com.example.looper_android.data.models

enum class LyricsSyncMode {
    LINE,
    WORD,
    CHAR;

    val settingValue: String get() = name.lowercase()

    companion object {
        fun fromSetting(value: String?): LyricsSyncMode = when (value?.lowercase()) {
            "line" -> LINE
            "word" -> WORD
            else -> CHAR
        }
    }
}
