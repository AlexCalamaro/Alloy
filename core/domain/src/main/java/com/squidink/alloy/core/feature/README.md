# Feature Registry

The Feature Registry provides a centralized system for managing all feature metadata in the Alloy app.

## Overview

The Feature Registry consolidates all feature information in one place, replacing scattered definitions across the codebase. This includes:

- Feature names, descriptions, and categories
- Navigation routes and icons
- Sort order and enabled states
- ViewModel factories for navigation

## Core Components

### FeatureDefinition

The `FeatureDefinition` data class contains all metadata for a feature:

```kotlin
data class FeatureDefinition(
    val id: String,                    // Unique identifier
    val name: String,                  // Display name
    val description: String,           // Feature description
    val screenRoute: String,           // Navigation route
    val category: String,              // Category for grouping
    val sortOrder: Int,                // Sort order within category
    val icon: ImageVector,             // Feature icon
    val detailFeatureProvider: (() -> FeatureDetail)?,  // Optional detail content
    val viewModelFactory: () -> ViewModel  // ViewModel for navigation
)
```

### FeatureRegistry

The `FeatureRegistry` provides centralized feature management:

```kotlin
// Get the singleton instance
val registry = featureRegistry()

// Register a feature
registry.registerFeature(featureDefinition)

// Get all features
val features = registry.getFeatures()

// Get features by category
val productivityFeatures = registry.getFeaturesByCategory("Productivity")

// Get enabled features sorted
val sortedFeatures = registry.getEnabledFeaturesSorted()

// Check if a feature is enabled
val isEnabled = registry.isFeatureEnabled("feature_id")

// Set a feature's enabled state
registry.setFeatureEnabled("feature_id", true)
```

## Feature Categories

Features are organized into categories:

- **System** (`FeatureCategories.SYSTEM`) - System-level features
- **Desktop** (`FeatureCategories.DESKTOP`) - Desktop workspace features  
- **Productivity** (`FeatureCategories.PRODUCTIVITY`) - Productivity tools

## Adding a New Feature

To add a new feature to the app:

1. **Create the feature module** in the `modules/` directory

2. **Register the feature** in `ModuleRegistryImpl.initializeFeatures()`:

```kotlin
featureRegistry.registerFeature(
    FeatureDefinition(
        id = "my_feature",
        name = "My Feature",
        description = "Description of functionality",
        screenRoute = "my_feature",
        category = FeatureCategories.PRODUCTIVITY,
        sortOrder = 3,
        icon = Icons.Default.SomeIcon,
        viewModelFactory = { MyFeatureViewModel() }
    )
)
```

3. **Add navigation** in `DashboardActivity`:

```kotlin
composable("my_feature") {
    val viewModel: MyFeatureViewModel = hiltViewModel()
    MyFeatureScreen(viewModel = viewModel)
}
```

4. **Implement FeatureDetail** for settings/detail content:

```kotlin
class MyFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        FeatureDetailSection("My Feature Settings") {
            // Settings content
        }
    }
}
```

## Migration from ModuleInfo

The old `ModuleInfo` class in `core/domain` is now deprecated. Use `FeatureDefinition` instead:

```kotlin
// Old (deprecated)
val info = ModuleInfo("my_feature", "My Feature", "Description", "my_feature")

// New
val feature = FeatureDefinition(
    id = "my_feature",
    name = "My Feature",
    description = "Description",
    screenRoute = "my_feature",
    category = "Productivity",
    sortOrder = 1,
    icon = Icons.Default.SomeIcon,
    viewModelFactory = { MyViewModel() }
)
```

## Feature States

Feature enabled states are persisted in DataStore using the key pattern:
`module_enabled_{featureId}`

The registry automatically reads and updates these states.

## Files

- `FeatureDefinition.kt` - The feature data class
- `FeatureRegistry.kt` - The registry implementation with utility extensions
- `IFeatureActions.kt` - Interface for feature actions
- `README.md` - This documentation
