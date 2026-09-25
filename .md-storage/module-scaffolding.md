# Module Scaffolding Tool

A bash script for quickly generating new feature modules in the Alloy Android project with consistent structure and best practices.

## Overview

The module scaffolding tool automates the creation of new feature modules following the Alloy architecture patterns:
- Domain-driven design with repository interfaces
- Hilt dependency injection
- Compose UI with ViewModel pattern
- Unit test scaffolding
- Gradle module configuration

**Location:** `tooling/scripts/createModule.sh`

## Usage

### Basic Module Creation

```bash
cd /home/alex/AndroidStudioProjects/Alloy
./tooling/scripts/createModule.sh -name=ModuleName -description="Module description"
```

### Options

| Option | Description | Required | Default |
|--------|-------------|----------|---------|
| `-name=` | Module name (must start with a letter) | Yes | - |
| `-description=` | Module description | No | "Module description" |
| `-room` | Include Room database dependencies | No | false |
| `-no-tests` | Exclude unit test generation | No | false |

### Examples

```bash
# Create a basic module
./tooling/module-scaffolding/createModule.sh -name=Settings -description="User settings management"

# Create a module with Room support
./tooling/module-scaffolding/createModule.sh -name=Profile -description="User profile management" -room

# Create a module without tests
./tooling/module-scaffolding/createModule.sh -name=Analytics -description="Analytics tracking" -no-tests
```

## Generated Structure

```
modules/<module-id>/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/squidink/alloy/modules/<module-id>/
    │       ├── di/
    │       │   └── <ModuleName>Module.kt
    │       ├── data/
    │       │   ├── I<ModuleName>Repository.kt
    │       │   └── <ModuleName>RepositoryImpl.kt
    │       ├── <ModuleName>ViewModel.kt
    │       └── ui/
    │           └── <ModuleName>Screen.kt
    └── test/
        └── java/com/squidink/alloy/modules/<module-id>/
            └── <ModuleName>ViewModelTest.kt
```

## Module Naming Rules

- Must start with a letter (A-Z, a-z)
- Can contain only letters and numbers
- Will be converted to lowercase with hyphens for the module ID
- Examples: `Settings` → `settings`, `UserProfile` → `user-profile`

## Post-Generation Steps

After generating a new module:

1. **Register the module** in `settings.gradle.kts`:
   ```kotlin
   include(":modules:<module-id>")
   ```

2. **Update the build** to verify the module compiles:
   ```bash
   ./gradlew :modules:<module-id>:build
   ```

3. **Implement the business logic**:
   - Define domain models in `core/domain`
   - Implement repository methods in `<ModuleName>RepositoryImpl.kt`
   - Add use cases if needed
   - Implement UI in `<ModuleName>Screen.kt`
   - Wire up dependency injection in `<ModuleName>Module.kt`

4. **Add navigation** to `core/navigation`:
   - Add screen route to `Screens.kt`
   - Add composable to `AlloyNavgraph.kt`

## Architecture Guidelines

### Repository Pattern

Repository interfaces define contracts without implementation details:

```kotlin
interface I<ModuleName>Repository {
    fun get<ModuleNames>(): Flow<List<DomainModel>>
    suspend fun insert<ModuleName>(item: DomainModel)
    // ... other methods
}
```

### ViewModel Pattern

ViewModels handle UI state only, delegating business logic to use cases:

```kotlin
data class <ModuleName>UiState(
    val isLoading: Boolean = false,
    // ... other state
) : UiState

sealed interface <ModuleName>UiAction : UiAction {
    data object Refresh : <ModuleName>UiAction
    // ... other actions
}
```

### Dependency Injection

DI modules provide dependencies without automatic binding:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object <ModuleName>Module {
    @Provides
    @Singleton
    fun provide<ModuleName>Repository(): I<ModuleName>Repository {
        return <ModuleName>RepositoryImpl(/* dependencies */)
    }
}
```

## Template Files

Template files are stored in `tooling/module-scaffolding/`:

- `createModule.sh` - Main generation script
- `createModule.kts` - Alternative Kotlin script (requires Kotlin runtime)

## Troubleshooting

### Module Already Exists Error

If you get "Module already exists" error, either:
- Use a different module name
- Delete the existing module directory: `rm -rf modules/<module-id>`

### Build Errors

After generating a module, if the build fails:
1. Verify the module is registered in `settings.gradle.kts`
2. Check that all imports are correct
3. Ensure domain models exist in `core/domain`

### Room Integration

When using `-room` flag, you'll need to:
1. Define data models as Room entities
2. Create DAO interfaces
3. Update the database module in `core/data`
4. Inject DAO into repository implementation

## Maintenance

### Updating Templates

To update the generated module structure:
1. Edit `createModule.sh`
2. Test with a sample module
3. Update documentation
4. Commit changes

### Adding New Features

To add new files or packages to generated modules:
1. Add directory creation in the script
2. Add file generation with appropriate content
3. Update the README template
4. Document the new structure

## Related Documentation

- [Domain Layer Architecture](./domain-layer-architecture.md)
- [Navigation Compose Guide](./navigation-compose-guide.md)
- [Module Contracts](./module-contracts.md)
