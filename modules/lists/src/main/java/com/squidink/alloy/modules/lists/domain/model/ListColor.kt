package com.squidink.alloy.modules.lists.domain.model

/**
 * Google Keep-style pastel color palette for notes and checklists.
 *
 * Provides complementary light and dark theme background colors ensuring legibility
 * and visual differentiation across themes.
 */
enum class ListColor(
    val colorHex: Long,
    val darkColorHex: Long,
    val displayName: String
) {
    DEFAULT(0x00000000L, 0x00000000L, "Default"),
    CORAL(0xFFF28B82L, 0xFF5C2B29L, "Coral"),
    PEACH(0xFFFBBC04L, 0xFF614A19L, "Peach"),
    SAND(0xFFFFF475L, 0xFF635D19L, "Sand"),
    MINT(0xFFCCFF90L, 0xFF345920L, "Mint"),
    SAGE(0xFFA7FFEBL, 0xFF16504BL, "Sage"),
    FOG(0xFFCBF0F8L, 0xFF2D555EL, "Fog"),
    STORM(0xFFAECBFAL, 0xFF1E3A5FL, "Storm"),
    DUSK(0xFFD7AEFBL, 0xFF42275EL, "Dusk"),
    BLOSSOM(0xFFFDCFE8L, 0xFF5B2245L, "Blossom"),
    CLAY(0xFFE6C9A8L, 0xFF442F19L, "Clay"),
    CHALK(0xFFE8EAEDL, 0xFF3C3F41L, "Chalk");

    companion object {
        fun fromHex(hex: Long): ListColor {
            return entries.find { it.colorHex == hex || it.darkColorHex == hex } ?: DEFAULT
        }
    }
}
