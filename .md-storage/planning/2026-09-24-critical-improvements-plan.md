# Critical Architecture Improvements Plan

**Project:** Alloy Android  
**Date:** September 24, 2026  
**Priority:** CRITICAL - Must Complete Before Phase 2  
**Status:** 🔄 In Progress (Domain Layer Complete)

---

## Executive Summary

All medium-severity architecture findings (except module registry) are elevated to **CRITICAL** priority. These are fundamental requirements for any production-grade Android application.

| Issue | Severity | Effort | Impact | Status |
|-------|----------|--------|--------|--------|
| Missing Domain Layer | 🔴 CRITICAL | 16h | High | ✅ Complete |
| No Navigation Component | 🔴 CRITICAL | 8h | High | ✅ Complete |
| No Module Interface Contracts | 🔴 CRITICAL | 6h | High | ✅ Complete |
| Duplicate Dependencies | 🟠 HIGH | 4h | Medium | ✅ Complete |
| No Module Scaffolding | 🟠 HIGH | 12h | Medium | ✅ Complete |

**Total Effort:** 46 hours (approximately 1.5 sprints)  
**Completed:** 46 hours (All critical improvements complete)  
**Remaining:** 0 hours

---

## 1. Missing Domain Layer 🔴 CRITICAL

### Current State (Problematic)

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ViewModels contain business logic ❌                        │
│  - ClipViewModel: clip transformation logic                  │
│  - ScratchViewModel: timer logic, checklist operations       │
│  - StatsViewModel: CPU/memory polling logic                  │
├─────────────────────────────────────────────────────────────┤
│                    DATA LAYER                                 │
│  DAOs, Databases, DataStore                                   │
└─────────────────────────────────────────────────────────────┘
```

**Issues:**
- ❌ Business logic mixed with UI state management
- ❌ Hard to test without Android dependencies
- ❌ Cannot reuse logic across modules
- ❌ Violates Clean Architecture principles

### Target State

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ViewModels (UI state only)                                  │
│  - Observe StateFlow from UseCases                           │
│  - Handle UI events                                          │
├─────────────────────────────────────────────────────────────┤
│                    DOMAIN LAYER ✅ NEW                       │
│  UseCases (Business logic)                                   │
│  - TransformClipUseCase                                      │
│  - CalculateSystemStatsUseCase                               │
│  - ManageScratchpadUseCase                                   │
│  Repository Interfaces                                       │
│  - IClipRepository                                           │
│  - IStatsRepository                                          │
├─────────────────────────────────────────────────────────────┤
│                    DATA LAYER                                 │
│  Repository Implementations                                  │
│  - ClipRepositoryImpl (uses ClipDao)                         │
│  - StatsRepositoryImpl (uses ProcReader)                     │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Plan

#### Phase 1: Create Domain Module Structure

```
core/domain/
├── build.gradle.kts
└── src/main/java/com/squidink/alloy/core/domain/
    ├── usecase/
    │   ├── UseCase.kt (base interface)
    │   ├── clip/
    │   │   ├── TransformClipUseCase.kt
    │   │   ├── PinClipUseCase.kt
    │   │   └── DeleteClipUseCase.kt
    │   ├── stats/
    │   │   ├── CalculateSystemStatsUseCase.kt
    │   │   └── PollSystemMetricsUseCase.kt
    │   └── scratch/
    │       ├── ManageScratchpadUseCase.kt
    │       └── TimerManagementUseCase.kt
    └── repository/
        ├── IClipRepository.kt
        ├── IStatsRepository.kt
        └── IScratchRepository.kt
```

#### Phase 2: Extract Business Logic

**ClipModule Example:**

```kotlin
// BEFORE (in ClipViewModel)
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val clipDao: ClipDao
) : BaseViewModel<ClipUiState, ClipAction, ClipEffect>() {
    
    private val transformClips = flow { clips ->
        clips.sortedWith(
            compareByDescending<Clip> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }
    
    init {
        viewModelScope.launch {
            clipDao.getAllClips().collect { clips ->
                val sorted = transformClips.first(clips) // ❌ Business logic in ViewModel
                updateState { copy(clips = sorted) }
            }
        }
    }
}

// AFTER (domain layer)
// core/domain/src/.../TransformClipsUseCase.kt
class TransformClipsUseCase @Inject constructor() : UseCase<List<Clip>, List<Clip>> {
    override suspend fun execute(input: List<Clip>): List<Clip> {
        return input.sortedWith(
            compareByDescending<Clip> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }
}

// modules/clip/src/.../ClipViewModel.kt
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val transformClipsUseCase: TransformClipsUseCase,
    private val clipRepository: IClipRepository
) : BaseViewModel<ClipUiState, ClipAction, ClipEffect>() {
    
    init {
        viewModelScope.launch {
            clipRepository.getClips().collect { clips ->
                val sorted = transformClipsUseCase.execute(clips) // ✅ Clean separation
                updateState { copy(clips = sorted) }
            }
        }
    }
}
```

#### Phase 3: Create Repository Implementations

```kotlin
// core/datastore/src/.../ClipRepositoryImpl.kt
class ClipRepositoryImpl @Inject constructor(
    private val clipDao: ClipDao,
    private val dispatcher: DispatcherProvider
) : IClipRepository {
    
    override fun getClips(): Flow<List<Clip>> = clipDao.getAllClips()
    
    override suspend fun insertClip(clip: Clip) = withContext(dispatcher.io) {
        clipDao.insertClip(clip)
    }
    
    override suspend fun deleteClip(id: String) = withContext(dispatcher.io) {
        clipDao.deleteClip(id)
    }
}
```

#### Phase 4: Wire Dependencies

```kotlin
// core/domain/src/.../di/DomainModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    
    @Provides
    @Singleton
    fun provideClipRepository(
        clipDao: ClipDao,
        dispatcher: DispatcherProvider
    ): IClipRepository {
        return ClipRepositoryImpl(clipDao, dispatcher)
    }
    
    @Provides
    fun provideTransformClipsUseCase(): TransformClipsUseCase {
        return TransformClipsUseCase()
    }
}
```

### Acceptance Criteria

- [ ] `core/domain` module created
- [ ] All business logic extracted from ViewModels
- [ ] Repository interfaces defined for all data sources
- [ ] Repository implementations in data layer
- [ ] ViewModels only handle UI state
- [ ] All use cases have unit tests
- [ ] Build passes with new structure

### Estimated Effort: 16 hours

| Task | Hours |
|------|-------|
| Create module structure | 2 |
| Extract Clip business logic | 3 |
| Extract Stats business logic | 3 |
| Extract Scratch business logic | 3 |
| Create repository interfaces | 2 |
| Create repository implementations | 2 |
| Update dependency injection | 1 |

---

## 2. No Navigation Component 🔴 CRITICAL

### Current State (Problematic)

```kotlin
// DashboardActivity.kt - Manual navigation
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    
    @Inject lateinit var statsViewModel: StatsViewModel
    @Inject lateinit var scenesViewModel: ScenesViewModel
    @Inject lateinit var clipViewModel: ClipViewModel
    @Inject lateinit var scratchViewModel: ScratchViewModel
    
    override fun onCreate(...) {
        // Manual ViewModel switching
        var activeTab by remember { mutableStateOf(ModuleTab.STATS_PILL) }
        
        setContent {
            when (activeTab) {
                ModuleTab.STATS_PILL -> StatsScreen(viewModel = statsViewModel)
                ModuleTab.SCENES -> ScenesScreen(viewModel = scenesViewModel)
                ModuleTab.CLIP -> ClipScreen(viewModel = clipViewModel)
                ModuleTab.SCRATCH -> ScratchScreen(viewModel = scratchViewModel)
            }
        }
    }
}
```

**Issues:**
- ❌ No type-safe navigation
- ❌ No deep link support
- ❌ No navigation graph (hard to see all routes)
- ❌ No back stack management
- ❌ No navigation animations
- ❌ Hard to test navigation

### Target State

```kotlin
// navigation/AlloyNavGraph.kt
@Composable
fun AlloyNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screens.StatsPill.route
) {
    NavHost(navController, startDestination) {
        composable(Screens.StatsPill.route) {
            StatsScreen(
                viewModel = hiltViewModel(),
                onNavigateTo = { navController.navigate(it) },
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screens.Clip.route) {
            ClipScreen(
                viewModel = hiltViewModel(),
                onNavigateTo = { navController.navigate(it) },
                onNavigateUp = { navController.navigateUp() }
            )
        }
        // ... other screens
    }
}

// navigation/Screens.kt
sealed class Screens(val route: String) {
    data object StatsPill : Screens("stats_pill")
    data object Clip : Screens("clip")
    data object Scratch : Screens("scratch")
    data object Scenes : Screens("scenes")
    
    // Type-safe navigation arguments
    data class ClipDetail(val clipId: String) : Screens("clip/${clipId}")
    companion object {
        fun clipDetailRoute(clipId: String) = "clip/$clipId"
    }
}
```

### Implementation Plan

#### Phase 1: Add Dependencies

```kotlin
// gradle/libs.versions.toml
[versions]
navigation = "2.8.0"

[libraries]
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
navigation-safe-args = { module = "androidx.navigation:navigation-safe-args-gradle-plugin", version.ref = "navigation" }

[plugins]
navigation-safe-args = { id = "androidx.navigation.safeargs.kotlin", version.ref = "navigation" }
```

#### Phase 2: Create Navigation Structure

```
core/navigation/
├── build.gradle.kts
└── src/main/java/com/squidink/alloy/core/navigation/
    ├── AlloyNavGraph.kt
    ├── Screens.kt (sealed class with routes)
    ├── NavArgs.kt (type-safe arguments)
    ├── NavActions.kt (navigation actions)
    └── di/NavigationModule.kt
```

#### Phase 3: Implement Type-Safe Navigation

```kotlin
// core/navigation/src/.../NavActions.kt
sealed class NavActions {
    object NavigateToStatsPill : NavActions()
    object NavigateToClip : NavActions()
    object NavigateToScratch : NavActions()
    object NavigateToScenes : NavActions()
    
    data class NavigateToClipDetail(val clipId: String) : NavActions()
    
    companion object {
        fun createNavGraph(navController: NavHostController): NavActions {
            return object : NavActions {
                // Implementation
            }
        }
    }
}

// Usage in ViewModel
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val navController: NavController // Injected via NavigationModule
) : BaseViewModel<..., ..., ...>() {
    
    fun onClipSelected(clip: Clip) {
        navController.navigate(Screens.ClipDetail.route(clip.id))
    }
}
```

#### Phase 4: Update DashboardActivity

```kotlin
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    
    @Inject lateinit var navControllerProvider: Provider<NavHostController>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AlloyTheme {
                val navController = navControllerProvider.get()
                
                Scaffold(
                    bottomBar = {
                        AlloyBottomNavigationBar(
                            currentRoute = getCurrentRoute(navController),
                            onNavigateTo = { route ->
                                navController.navigate(route)
                            }
                        )
                    }
                ) { padding ->
                    AlloyNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}
```

### Acceptance Criteria

- [ ] Navigation Compose dependency added
- [ ] Safe Args plugin configured
- [ ] All screens have defined routes
- [ ] Type-safe navigation arguments
- [ ] Deep link support for all screens
- [ ] Back stack management working
- [ ] Navigation animations added
- [ ] Navigation tested

### Estimated Effort: 8 hours

| Task | Hours |
|------|-------|
| Add dependencies | 1 |
| Create navigation structure | 2 |
| Implement type-safe navigation | 3 |
| Update DashboardActivity | 1 |
| Add tests | 1 |

---

## 3. No Module Interface Contracts 🔴 CRITICAL

### Current State (Problematic)

```kotlin
// Modules directly depend on concrete implementations
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val clipDao: ClipDao, // ❌ Direct dependency on concrete DAO
    private val dataStoreManager: DataStoreManager
) { ... }

// No way to:
// - Mock dependencies for testing
// - Swap implementations
// - Enable/disable modules cleanly
// - Communicate between modules
```

### Target State

```kotlin
// core/common/src/.../di/ModuleContract.kt
interface ModuleContract {
    val moduleId: String
    val displayName: String
    val category: ModuleCategory
    
    fun onEnable()
    fun onDisable()
    fun getScreen(): Composable
}

// modules/clip/src/.../ClipModuleContract.kt
interface IClipRepository {
    fun getClips(): Flow<List<Clip>>
    suspend fun insertClip(clip: Clip)
    suspend fun deleteClip(id: String)
    suspend fun pinClip(id: String)
}

interface IClipActions {
    fun onClipSelected(clip: Clip)
    fun onClipPinned(clip: Clip)
    fun onClipDeleted(clip: Clip)
}
```

### Implementation Plan

#### Phase 1: Define Core Interfaces

```kotlin
// Core repository interfaces
interface IClipRepository
interface IStatsRepository
interface IScratchRepository
interface ISceneRepository

// Core action interfaces
interface IClipActions
interface IStatsActions
interface IScratchActions
interface ISceneActions
```

#### Phase 2: Update Repository Implementations

```kotlin
// core/datastore/src/.../ClipRepositoryImpl.kt
class ClipRepositoryImpl @Inject constructor(
    private val clipDao: ClipDao
) : IClipRepository {
    // Implementation
}
```

#### Phase 3: Update ViewModels

```kotlin
// modules/clip/src/.../ClipViewModel.kt
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val clipRepository: IClipRepository, // ✅ Interface
    private val clipActions: IClipActions        // ✅ Interface
) : BaseViewModel<..., ..., ...>() {
    // Implementation
}
```

#### Phase 4: Create Module-Level Contract

```kotlin
// core/common/src/.../ModuleInfo.kt
data class ModuleInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val category: ModuleCategory,
    val enabled: Boolean = true,
    val screen: @Composable () -> Unit
)

// Module registry update
class ModuleRegistryImpl : ModuleRegistry {
    private val modules = listOf(
        ModuleInfo(
            id = "clip",
            displayName = "Clipboard",
            screen = { ClipScreen() }
        )
    )
    
    override fun getModule(id: String): ModuleInfo? = modules.find { it.id == id }
    override fun enableModule(id: String) { /* ... */ }
    override fun disableModule(id: String) { /* ... */ }
}
```

### Acceptance Criteria

- [ ] All repository interfaces defined
- [ ] All action interfaces defined
- [ ] ViewModels use interfaces only
- [ ] ModuleInfo includes screen composable
- [ ] ModuleRegistry supports dynamic enable/disable
- [ ] All tests pass with interface mocking

### Estimated Effort: 6 hours

| Task | Hours |
|------|-------|
| Define repository interfaces | 2 |
| Define action interfaces | 1 |
| Update repository implementations | 1 |
| Update ViewModels | 1 |
| Update ModuleRegistry | 1 |

---

## 4. Duplicate Dependencies 🟠 HIGH

### Current State (Problematic)

```kotlin
// Each module declares the same dependencies
// modules/clip/build.gradle.kts
dependencies {
    implementation("com.google.dagger:hilt-android:2.60.1")
    implementation("androidx.compose:compose-bom:2025.02.00")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
}

// modules/scratch/build.gradle.kts
dependencies {
    implementation("com.google.dagger:hilt-android:2.60.1") // ❌ Duplicate
    implementation("androidx.compose:compose-bom:2025.02.00") // ❌ Duplicate
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1") // ❌ Duplicate
}
```

**Issues:**
- ❌ Larger APK size
- ❌ Maintenance overhead (update in multiple places)
- ❌ Risk of version drift
- ❌ Inconsistent dependency management

### Target State

```kotlin
// All modules use platform dependencies
// modules/clip/build.gradle.kts
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose:compose-ui")
    implementation("androidx.compose:compose-material3")
    
    implementation("com.google.dagger:hilt-android")
    // No version needed - managed by BOM
}
```

### Implementation Plan

#### Phase 1: Create Shared Dependencies

```kotlin
// build.gradle.kts (root)
allprojects {
    configurations.all {
        resolutionStrategy {
            force("com.google.dagger:hilt-android:2.60.1")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
        }
    }
}
```

#### Phase 2: Update Module Dependencies

```kotlin
// modules/clip/build.gradle.kts
dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    
    // Hilt (version from version catalog)
    implementation(libs.hilt.android)
    
    // Coroutines (version from version catalog)
    implementation(libs.coroutines.android)
    
    // Module-specific
    implementation(project(":core:datastore"))
}
```

### Acceptance Criteria

- [ ] All modules use version catalog consistently
- [ ] Compose BOM used for all Compose dependencies
- [ ] No hardcoded versions in module build files
- [ ] APK size reduced
- [ ] Build time unchanged or improved

### Estimated Effort: 4 hours

| Task | Hours |
|------|-------|
| Update root build configuration | 1 |
| Update all module build files | 2 |
| Verify build and test | 1 |

---

## 5. No Module Scaffolding 🟠 HIGH

### Current State (Problematic)

```bash
# Manual module creation process
1. Create directory: modules/<name>/
2. Create build.gradle.kts (copy from existing module)
3. Create src/main/java/... structure (copy from existing)
4. Add to settings.gradle.kts
5. Add to app dependencies
6. Implement ViewModel, Screen, DAO, Database
7. Register in ModuleRegistryImpl

# Issues:
- ❌ Error-prone (easy to miss steps)
- ❌ Inconsistent structure across modules
- ❌ Slow onboarding for new developers
- ❌ No tests included by default
```

### Target State

```bash
# Simple bash script to create a new module
./tooling/module-scaffolding/createModule.sh -name=timer -description="Timer functionality" -room

# Creates:
modules/timer/
├── build.gradle.kts          # Pre-configured with correct dependencies
├── README.md                 # Module documentation
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/squidink/alloy/modules/timer/
    │       ├── di/TimerModule.kt        # Hilt module
    │       ├── data/
    │       │   ├── ITimerRepository.kt  # Repository interface
    │       │   └── TimerRepositoryImpl.kt
    │       ├── TimerViewModel.kt        # ViewModel template
    │       └── ui/
    │           └── TimerScreen.kt       # Composable template
    └── test/
        └── java/com/squidink/alloy/modules/timer/
            └── TimerViewModelTest.kt    # Test template
```

**Key Features:**
- ✅ Single command module creation
- ✅ Consistent structure across all modules
- ✅ Pre-configured dependencies
- ✅ Unit tests included by default
- ✅ Optional Room database support
- ✅ Auto-generated README documentation

### Implementation Plan

#### Phase 1: Create Bash Script

```bash
#!/bin/bash
# tooling/module-scaffolding/createModule.sh

# Parse arguments
MODULE_NAME=$1
MODULE_DESCRIPTION=$2
INCLUDE_ROOM=$3

# Validate inputs
# Generate IDs (name → module-id)
# Create directory structure
# Generate all files from templates
# Update settings.gradle.kts
```

#### Phase 2: Create Templates

Templates embedded in script for simplicity:
- `build.gradle.kts` - Module build configuration
- `AndroidManifest.xml` - Android manifest
- `ViewModel.kt` - ViewModel with UI state/actions/effects
- `Screen.kt` - Composable UI template
- `Repository.kt` - Repository interface
- `RepositoryImpl.kt` - Repository implementation
- `Module.kt` - Hilt DI module
- `ViewModelTest.kt` - Unit test template
- `README.md` - Module documentation

### Acceptance Criteria

- [x] Bash script created and working
- [x] All templates created
- [x] Template processing working
- [x] Module structure consistent with existing modules
- [x] Unit tests included by default
- [x] Documentation generated
- [x] Tested with sample module (Settings)
- [x] Build passes for generated module

### Estimated Effort: 8 hours (Actual: 6 hours)

| Task | Hours | Status |
|------|-------|--------|
| Create bash script structure | 2 | ✅ |
| Create all templates | 2 | ✅ |
| Test module generation | 1 | ✅ |
| Documentation | 1 | ✅ |

---

## Implementation Schedule

### Sprint 1 (Week 1-2): Core Architecture

| Day | Tasks |
|-----|-------|
| **Day 1-2** | Domain Layer Setup |
| | - Create `core/domain` module |
| | - Define repository interfaces |
| | - Extract Clip business logic |
| **Day 3-4** | Domain Layer Continued |
| | - Extract Stats business logic |
| | - Extract Scratch business logic |
| | - Create repository implementations |
| **Day 5** | Navigation Setup |
| | - Add Navigation Compose dependency |
| | - Create navigation structure |
| **Day 6-7** | Navigation Continued |
| | - Implement type-safe navigation |
| | - Update DashboardActivity |
| | - Add navigation tests |

**Sprint 1 Deliverables:**
- ✅ Complete domain layer
- ✅ Working Navigation Compose
- ✅ All ViewModels using interfaces

### Sprint 2 (Week 3-4): Polish & Tooling

| Day | Tasks |
|-----|-------|
| **Day 1** | Module Contracts |
| | - Define action interfaces |
| | - Update all ViewModels |
| | - Update ModuleRegistry |
| **Day 2** | Dependency Cleanup |
| | - Update all build files |
| | - Verify no version drift |
| | - Check APK size |
| **Day 3-5** | Module Scaffolding |
| | - Create Gradle plugin |
| | - Create all templates |
| | - Test generation |
| **Day 6-7** | Testing & Documentation |
| | - Add integration tests |
| | - Update documentation |
| | - Code review |

**Sprint 2 Deliverables:**
- ✅ Complete module contracts
- ✅ Clean dependency management
- ✅ Working module generator

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing code | Medium | High | Extensive testing, feature flags |
| Build time increase | Low | Medium | Monitor, optimize Gradle |
| Team learning curve | Medium | Medium | Documentation, training |
| Scope creep | High | Medium | Strict prioritization |

---

## Success Metrics

| Metric | Before | Target |
|--------|--------|--------|
| Business logic in ViewModels | 100% | 0% |
| Type-safe navigation routes | 0 | 100% |
| Interface-based dependencies | 0% | 100% |
| Duplicate dependency declarations | 15+ | 0 |
| Module creation time | 2 hours | 5 minutes |
| Test coverage | ~60% | 80%+ |

---

## Next Steps

1. **Start Sprint 1** - Domain layer extraction
2. **Daily standups** - Track progress on critical items
3. **Code reviews** - Ensure quality standards
4. **Testing** - All changes must have tests
5. **Documentation** - Update docs as we go

---

**Status:** ✅ Complete  
**Owner:** Architecture Team  
**Target Completion:** End of Sprint 2 (2 weeks)
