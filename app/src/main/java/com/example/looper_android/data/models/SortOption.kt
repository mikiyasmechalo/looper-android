package com.example.looper_android.data.models

enum class SortOption(val label: String) {
    DEFAULT("Default Order"),
    TITLE_ASC("Title (A - Z)"),
    TITLE_DESC("Title (Z - A)"),
    ARTIST_ASC("Artist (A - Z)"),
    ALBUM_ASC("Album (A - Z)"),
    DURATION("Duration"),
    YEAR_NEWEST("Year (Newest)"),
    YEAR_OLDEST("Year (Oldest)")
}
