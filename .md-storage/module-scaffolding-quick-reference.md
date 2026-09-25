# Module Scaffolding Quick Reference

## Creating a New Module

### Basic Usage

```bash
cd /home/alex/AndroidStudioProjects/Alloy

# Create a basic module
./tooling/scripts/createModule.sh -name=ModuleName -description="Module description"

# Examples
./tooling/scripts/createModule.sh -name=Settings -description="User settings"
./tooling/scripts/createModule.sh -name=Profile -description="User profile" -room
./tooling/scripts/createModule.sh -name=Analytics -description="Analytics" -no-tests
```

### Options

| Flag | Description | Example |
|------|-------------|---------|
| `-name=` | Module name (required) | `-name=Settings` |
| `-description=` | Module description | `-description="User settings"` |
| `-room` | Include Room database | `-room` |
| `-no-tests` | Skip test generation | `-no-tests` |

### Naming Rules

- Must start with a letter (A-Z, a-z)
- Can contain only letters and numbers
- Converted to lowercase with hyphens for module ID

**Examples:**
- `Settings` → `settings`
- `UserProfile` → `user-profile`
- `MyModule123` → `mymodule123`

---

## Post-Generation Steps

### 1. Register the Module

Add to `settings.gradle.kts`:

```kotlin
include(":modules:<module-id>")
```

### 2. Build and Verify

```bash
./gradlew :modules:<module-id>:build
```

### 3. Implement the Module

#### Define Domain Models

```kotlin
// core/domain/src/.../model/YourModel.kt
data class YourModel(
    val id: String,
    val name: String,
    // ...
)
```

#### Update Repository Interface

```kotlin
// core/domain/src/.../repository/IYourModuleRepository.kt
interface IYourModuleRepository {
    fun getYourModels(): Flow<List<YourModel>>
    suspend fun insertYourModel(model: YourModel)
    // ...
}
```

#### Implement Repository

```kotlin
// modules/<module-id>/src/.../data/YourModuleRepositoryImpl.kt
class YourModuleRepositoryImpl @Inject constructor(
    private val yourModelDao: YourModelDao
) : IYourModuleRepository {
    // Implement methods
}
```

#### Wire DI

```kotlin
// modules/<module-id>/src/.../di/YourModuleModule.kt
@Module
@InstallIn(SingletonComponent::class)
object YourModuleModule {
    @Provides
    @Singleton
    fun provideYourModuleRepository(
        dao: YourModelDao
    ): IYourModuleRepository {
        return YourModuleRepositoryImpl(dao)
    }
}
```

#### Implement ViewModel Logic

```kotlin
// modules/<module-id>/src/.../YourModuleViewModel.kt
class YourModuleViewModel @Inject constructor(
    private val repository: IYourModuleRepository
) : BaseViewModel<YourModuleUiState, YourModuleUiAction, YourModuleUiEffect>() {
    
    init {
        viewModelScope.launch {
            repository.getYourModels().collect { models ->
                updateState { copy(items = models) }
            }
        }
    }
}
```

#### Build UI

```kotlin
// modules/<module-id>/src/.../ui/YourModuleScreen.kt
@Composable
fun YourModuleScreen(
    viewModel: YourModuleViewModel,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Build your UI
}
```

### 4. Add Navigation

```kotlin
// core/navigation/src/.../Screens.kt
data object YourModule : Screens("your_module")

// core/navigation/src/.../AlloyNavGraph.kt
composable(Screens.YourModule.route) {
    YourModuleScreen(
        viewModel = hiltViewModel(),
        onNavigateTo = { navController.navigate(it) },
        onNavigateUp = { navController.navigateUp() }
    )
}
```

---

## Troubleshooting

### Module Already Exists

```bash
rm -rf modules/<module-id>
./tooling/scripts/createModule.sh -name=ModuleName ...
```

### Build Errors

```bash
# Clean and rebuild
./gradlew clean :modules:<module-id>:build

# Check for compilation errors
./gradlew :modules:<module-id>:compileDebugKotlin
```

### Import Errors

Ensure package names match:
- Module package: `com.squidink.alloy.modules.<module-id>`
- ViewModel: `<ModuleName>ViewModel`
- Repository: `I<ModuleName>Repository`

### IDE Gradle Sync Issue (Fixed)

**Problem:** "Unsupported Kotlin plugin version" warning during IDE sync

**Root Cause:** The `tooling:module-scaffolding` Gradle plugin module used `kotlin-dsl` which conflicts with the project's Kotlin 2.0.21 version (Gradle 9.7.1 uses embedded Kotlin 2.4.0).

**Solution:** Removed the Gradle plugin module entirely. The bash script approach is simpler and doesn't require Gradle plugin infrastructure.

**Changes Made:**
- Removed `tooling:module-scaffolding` from `settings.gradle.kts`
- Moved `createModule.sh` to `tooling/scripts/`
- Added `kotlin.jvm.target.validation.mode=IGNORE` to `gradle.properties` to suppress JDK 25 warnings

### JDK Version Warnings

**Note:** The project uses JDK 25 (from Android Studio Canary). Kotlin 2.0.21 shows warnings about JDK 25 support, but these are informational only and don't affect builds.

**Suppression:** Added `kotlin.jvm.target.validation.mode=IGNORE` to `gradle.properties`

---

## Usage Examples

```bash
# Create a basic module
./tooling/scripts/createModule.sh -name=Settings -description="User settings"

# Create a module with Room support
./tooling/scripts/createModule.sh -name=Profile -description="User profile" -room

# Create a module without tests
./tooling/scripts/createModule.sh -name=Analytics -description="Analytics" -no-tests
```

## File Structure Reference

```
modules/<module-id>/
├── build.gradle.kts          # ✅ Auto-generated
├── README.md                 # ✅ Auto-generated
└── src/
    ├── main/
    │   ├── AndroidManifest.xml   # ✅ Auto-generated
    │   └── java/com/squidink/alloy/modules/<module-id>/
    │       ├── di/
    │       │   └── <ModuleName>Module.kt  # ✅ Template
    │       ├── data/
    │       │   ├── I<ModuleName>Repository.kt  # ✅ Template
    │       │   └── <ModuleName>RepositoryImpl.kt  # ✅ Template
    │       ├── <ModuleName>ViewModel.kt  # ✅ Template
    │       └── ui/
    │           └── <ModuleName>Screen.kt  # ✅ Template
    └── test/
        └── <ModuleName>ViewModelTest.kt  # ✅ Template (if enabled)
```

---

## Template Files

All templates are in `tooling/module-scaffolding/createModule.sh`:

| Template | Purpose |
|----------|---------|
| `build.gradle.kts` | Module dependencies |
| `AndroidManifest.xml` | Android manifest |
| `ViewModel.kt` | ViewModel with state/actions/effects |
| `Screen.kt` | Composable UI |
| `Repository.kt` | Repository interface |
| `RepositoryImpl.kt` | Repository implementation |
| `Module.kt` | Hilt DI module |
| `ViewModelTest.kt` | Unit test |
| `README.md` | Documentation |

---

## Best Practices

1. **Use domain models** - Define models in `core/domain`
2. **Interface-based** - Always use repository interfaces
3. **Test first** - Generated tests provide a starting point
4. **Keep it simple** - Start with basic functionality
5. **Document** - Update README with module-specific info

---

## Related Documentation

- [Module Scaffolding Guide](./module-scaffolding.md)
- [Architecture Improvements Completion](./architecture-improvements-completion.md)
- [Critical Improvements Plan](./planning/2026-09-24-critical-improvements-plan.md)
