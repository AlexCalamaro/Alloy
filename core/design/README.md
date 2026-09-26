# Alloy Theming Guide

## Material You (Dynamic Color) Support

Alloy fully supports Material You (Android 12+/API 31+) dynamic color theming. The app will automatically generate a color scheme based on the user's wallpaper when enabled.

### How It Works

1. **Dynamic Color Generation**: On Android 12+, the system extracts dominant colors from the user's wallpaper
2. **Automatic Application**: These colors are applied to all UI components automatically
3. **User Control**: Users can toggle dynamic colors on/off via Settings (to be implemented)

### Architecture

#### Core Theme Component
Located in `src/main/java/com/squidink/alloy/core/design/Theme.kt`:

```kotlin
@Composable
fun AlloyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Material You support
    content: @Composable () -> Unit
)
```

**Parameters:**
- `darkTheme`: Follows system dark mode preference
- `dynamicColor`: Enable/disable Material You dynamic colors (defaults to `true`)
- `content`: The composable content to wrap with the theme

#### Color Scheme Definitions

**Light Theme Colors:**
- Primary: `#6200EE` (Purple)
- Primary Container: `#BB86FC` (Light Purple)
- Secondary: `#03DAC6` (Teal)
- Background: `#FFFFFF` (White)
- Surface: `#FFFFFF` (White)
- Error: `#B00020` (Red)

**Dark Theme Colors:**
- Primary: `#BB86FC` (Light Purple)
- Primary Container: `#6200EE` (Purple)
- Secondary: `#03DAC6` (Teal)
- Background: `#121212` (Dark Gray)
- Surface: `#1E1E1E` (Lighter Dark Gray)
- Error: `#CF6679` (Light Red)

#### Status Colors (Available Everywhere)
```kotlin
val SuccessColor = Color(0xFF4CAF50)  // Green
val WarningColor = Color(0xFFFF9800)  // Orange
val ErrorColor = Color(0xFFB00020)    // Red
val InfoColor = Color(0xFF2196F3)     // Blue
val DownloadColor = Color(0xFF4CAF50) // Green (network)
val UploadColor = Color(0xFF2196F3)   // Blue (network)
```

### User Preferences

Dynamic color preference is stored in encrypted preferences via `DataStoreManager`:

```kotlin
// Get current preference (defaults to true)
val dynamicColorEnabled: Flow<Boolean> = dataStoreManager.getDynamicColor()

// Set preference
dataStoreManager.setDynamicColor(enabled = false)
```

**Preference Key:** `dynamic_color_enabled`
**Default Value:** `true` (Material You enabled)

### Usage in Modules

All module UIs automatically inherit the theme from `AlloyTheme`:

```kotlin
// In your Activity
setContent {
    AlloyTheme(dynamicColor = useDynamicColors) {
        YourModuleScreen()
    }
}
```

### Reusable UI Components

#### Setting Cards (`SettingCards.kt`)

Reusable composable components for consistent settings UI:

**`SettingSection`** - Container for a settings section with title:
```kotlin
@Composable
fun SettingSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

**`InfoCard`** - Info card for tips, warnings, or helpful information:
```kotlin
@Composable
fun InfoCard(
    message: String,
    modifier: Modifier = Modifier
)
```

**`SettingCard`** - Card for displaying a single setting:
```kotlin
@Composable
fun SettingCard(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
)
```

**`FeatureDetailSection`** - Container for feature detail content:
```kotlin
@Composable
fun FeatureDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

#### Spacing Constants (`Spacing.kt`)

Standard spacing values and application constants:

```kotlin
@Stable
object Spacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
}

object AppConstants {
    const val POLLING_INTERVAL_MS = 1000L
    const val DEFAULT_CACHE_SIZE = 100
    const val BATTERY_SCALE_DEFAULT = 100
    const val MAX_HISTORY_ITEMS = 50
}
```

### Color Resource Files

#### `src/main/res/values/colors.xml`
Contains XML color resources for non-Compose usage:

```xml
<color name="alloy_primary">#6200EE</color>
<color name="alloy_secondary">#03DAC6</color>
<color name="alloy_success">#4CAF50</color>
<!-- ... more colors -->
```

#### `src/main/res/values/strings.xml`
Contains all user-facing strings for localization:

```xml
<string name="clip_title">Clipboard Workbench</string>
<string name="scenes_title">Workspace Scenes Manager</string>
<!-- ... more strings -->
```

### Best Practices

1. **Always Use ColorScheme**: Prefer `MaterialTheme.colorScheme.primary` over hardcoded colors
2. **Respect Theme Mode**: Use `isSystemInDarkTheme()` for dark mode detection
3. **Status Colors**: Use the predefined status colors for consistent feedback
4. **Localization**: All user-facing strings must use `stringResource()`
5. **Testing**: Test both with and without dynamic colors enabled

### Migration Guide

#### For New Modules
1. Import `MaterialTheme` from `androidx.compose.material3`
2. Use `MaterialTheme.colorScheme.*` for colors
3. Use `MaterialTheme.typography.*` for text styles
4. Wrap your screen in `AlloyTheme` at the activity level

#### For Existing Modules
1. Replace hardcoded `Color(...)` values with `MaterialTheme.colorScheme.*`
2. Replace string literals with `stringResource(R.string.*))`
3. Ensure proper imports for `stringResource`

### Future Enhancements

- [ ] Add user toggle in Settings UI
- [ ] Support custom color palettes
- [ ] Add theme preview in Settings
- [ ] Support per-module theme overrides
- [ ] Add animation for theme transitions

### References

- [Material You Design Guidelines](https://m3.material.io/)
- [Android Dynamic Colors](https://developer.android.com/guide/topics/ui/look-and-feel/dynamic-colors)
- [Compose Material3 Documentation](https://developer.android.com/jetpack/compose/theme)
