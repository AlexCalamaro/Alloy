# Android Development Best Practices

**This document is binding for all development work in this project.**  
All code changes must comply with these guidelines. Violations will be caught by code review.

---

## Table of Contents

1. [Architecture & Code Organization](#1-architecture--code-organization)
2. [Kotlin Best Practices](#2-kotlin-best-practices)
3. [Jetpack Compose Patterns](#3-jetpack-compose-patterns)
4. [Dependency Injection](#4-dependency-injection)
5. [Testing Standards](#5-testing-standards)
6. [Error Handling](#6-error-handling)
7. [Performance Considerations](#7-performance-considerations)
8. [Security Best Practices](#8-security-best-practices)
9. [Memory Management](#9-memory-management)
10. [Documentation Standards](#10-documentation-standards)
11. [Generated Documentation Management](#11-generated-documentation-management)
12. [Third-Party Dependency License Requirements](#12-third-party-dependency-license-requirements)

---

## 1. Architecture & Code Organization

### 1.1 Clean Architecture Layers

```
app/                          # Application entry point
├── src/main/java/
    ├── di/                   # Hilt modules
    └── AlloyApplication.kt

core/                         # Shared libraries
├── common/                   # Base classes, utilities
├── design/                   # UI components, themes
├── datastore/                # DataStore, encryption
├── proc/                     # Process management
└── netlocal/                 # Network utilities

modules/                      # Feature modules
├── clip/                     # Clipboard manager
│   ├── data/                 # DAOs, Database, Repositories
│   ├── domain/               # UseCases, Models
│   └── presentation/         # ViewModels, UI
├── statspill/
├── scenes/
└── scratch/
```

**Rule**: Each feature module must follow this structure. Dependencies flow inward:
- `presentation` → `domain` → `data`
- `domain` has **no dependencies** on other layers
- `data` implements interfaces defined in `domain`

### 1.2 Package Naming

```kotlin
// Feature module structure
package com.squidink.alloy.modules.clip

// Data layer
package com.squidink.alloy.modules.clip.data
package com.squidink.alloy.modules.clip.data.local    // DAOs, Database
package com.squidink.alloy.modules.clip.data.remote   // API clients

// Domain layer
package com.squidink.alloy.modules.clip.domain
package com.squidink.alloy.modules.clip.domain.usecase

// Presentation layer
package com.squidink.alloy.modules.clip.presentation
package com.squidink.alloy.modules.clip.presentation.viewmodel
package com.squidink.alloy.modules.clip.presentation.ui
```

### 1.3 File Organization

Each file should follow this order:

```kotlin
package com.example.module

// 1. Imports (grouped: standard library, Android, third-party, project)
import kotlin.*
import android.*
import dagger.*
import com.squidink.alloy.*

// 2. Constants (file-level if used by multiple classes)
private const val TAG = "ClassName"
private const val DEFAULT_TIMEOUT_MS = 5000L

// 3. Type aliases / Extension functions (if any)
typealias ClipId = String

// 4. Main class (single responsibility)
class ClassName {
    // Nested types, properties, methods in order
}

// 5. Companion object (if needed)
companion object {
    // Factory methods, constants
}

// 6. Sealed classes / Interfaces (if applicable)
sealed interface Event

// 7. Data classes (if applicable)
data class Model(...)
```

---

## 2. Kotlin Best Practices

### 2.1 Null Safety

```kotlin
// ✅ GOOD
val name: String = getUser()?.name ?: "Unknown"
val count: Int = items?.size ?: 0

// ❌ BAD - Never use !! unless absolutely necessary
val name: String = getUser()!!.name

// ✅ GOOD - Use let for safe operations
getUser()?.let { user ->
    updateUI(user.name)
}

// ✅ GOOD - Use run for transformations
val displayName = getUser()?.run { "$firstName $lastName" } ?: "Guest"
```

### 2.2 Immutability

```kotlin
// ✅ GOOD - Prefer val over var
val items: List<String> = listOf("a", "b", "c")

// ✅ GOOD - Use data classes for models
data class ClipEntity(
    val id: String,
    val textContent: String,
    val timestamp: Long
)

// ✅ GOOD - Use immutable collections
val immutableList: List<String> = listOf("a", "b")
val immutableMap: Map<String, Int> = mapOf("key" to 1)

// ❌ BAD - Avoid mutable state when possible
var counter: Int = 0  // Only use when truly needed
```

### 2.3 Type Inference

```kotlin
// ✅ GOOD - Let compiler infer when obvious
val message = "Hello"
val items = listOf("a", "b", "c")

// ✅ GOOD - Be explicit when clarity requires it
val userId: String? = null
val timeout: Long = 5000L

// ❌ BAD - Don't over-specify types
val message: String = "Hello"  // Unnecessary unless needed for clarity
```

### 2.4 Scope Functions

```kotlin
// ✅ GOOD - Use when it improves readability
user?.let { updateUI(it) }           // Pass to function
user?.also { log("Created: $it") }   // Side effect
config?.run { saveToDisk() }         // Object context
list?.forEach { process(it) }        // Collection iteration

// ❌ BAD - Avoid when it reduces clarity
val x = obj.apply { prop = value }   // Only use for builder patterns
```

### 2.5 Extensions

```kotlin
// ✅ GOOD - Keep extension functions focused and useful
fun String.toMd5(): String = /* ... */
fun Context.showToast(message: String) = /* ... */

// ❌ BAD - Don't extend types with unrelated functionality
fun String.navigateToScreen() = /* ... */  // Use a proper navigator instead
```

### 2.6 Sealed Types

```kotlin
// ✅ GOOD - Use sealed classes for state machines
sealed interface UiState {
    data object Loading : UiState
    data class Success(val data: List<Clip>) : UiState
    data class Error(val message: String) : UiState
}

// ✅ GOOD - Exhaustive when statements
when (state) {
    is UiState.Loading -> showLoading()
    is UiState.Success -> showContent(state.data)
    is UiState.Error -> showError(state.message)
}

// ❌ BAD - Avoid sealed hierarchies with > 5 branches
```

---

## 3. Jetpack Compose Patterns

### 3.1 Unidirectional Data Flow (UDF)

```kotlin
// ✅ GOOD - State hoisting
@Composable
fun ClipList(
    clips: List<Clip>,
    onClipSelected: (Clip) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(clips) { clip ->
            ClipItem(
                clip = clip,
                onClick = { onClipSelected(clip) }
            )
        }
    }
}

// ❌ BAD - Don't hold state inside reusable composables
@Composable
fun BadClipList() {
    var selectedClip by remember { mutableStateOf<Clip?>(null) }  // Wrong!
    // ...
}
```

### 3.2 State Management

```kotlin
// ✅ GOOD - Use derivedStateOf for computed values
@Composable
fun ClipList(clips: List<Clip>) {
    val filteredClips by remember(clips) {
        derivedStateOf { clips.filter { it.isPinned } }
    }
    
    LazyColumn {
        items(filteredClips) { /* ... */ }
    }
}

// ✅ GOOD - Use remember for expensive operations
@Composable
fun ClipItem(clip: Clip) {
    val formattedDate = remember(clip.timestamp) {
        formatDate(clip.timestamp)
    }
    Text(formattedDate)
}

// ❌ BAD - Don't create state in loops
LazyColumn {
    items(100) { index ->
        var count by remember { mutableStateOf(0) }  // Creates 100 states!
    }
}
```

### 3.3 Side Effects

```kotlin
// ✅ GOOD - Use LaunchedEffect for coroutine side effects
@Composable
fun ClipViewModelWrapper(viewModel: ClipViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadClips()
    }
    
    ClipList(state = uiState)
}

// ✅ GOOD - Use DisposableEffect for cleanup
@Composable
fun Timer(viewModel: ClipViewModel) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTimer()
        }
    }
}

// ❌ BAD - Don't call suspend functions directly in Composable body
@Composable
fun BadComposable() {
    loadData()  // Compile error!
}
```

### 3.4 Stable Types

```kotlin
// ✅ GOOD - Use @Stable annotation
@Stable
fun formatDate(timestamp: Long): String = /* ... */

// ✅ GOOD - Use data classes (automatically stable)
data class ClipUiState(
    val clips: List<Clip>,
    val selectedClip: Clip?
)

// ❌ BAD - Avoid unstable types as parameters
@Composable
fun BadComposable(state: ClipUiState) {  // May cause unnecessary recompositions
    // ...
}

// ✅ GOOD - Use rememberSaveable for persisted state
@Composable
fun SearchBox() {
    var query by rememberSaveable { mutableStateOf("") }
    // Survives configuration changes
}
```

### 3.5 Lazy Lists

```kotlin
// ✅ GOOD - Always use keys for item stability
LazyColumn {
    items(clips, key = { it.id }) { clip ->
        ClipItem(clip = clip)
    }
}

// ✅ GOOD - Use itemsIndexed when index is needed
LazyColumn {
    itemsIndexed(clips) { index, clip ->
        ClipItem(clip = clip, showDivider = index < clips.lastIndex)
    }
}

// ❌ BAD - Never use index as key
LazyColumn {
    items(clips, key = { index -> index }) { clip ->  // Wrong!
        // Causes recomposition issues
    }
}
```

### 3.6 Modifier Composition

```kotlin
// ✅ GOOD - Apply modifiers in order
@Composable
fun ClipItem(
    clip: Clip,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(clip) }
            .padding(8.dp)
    ) {
        // ...
    }
}

// ❌ BAD - Don't create Modifier in loops
LazyColumn {
    items(items) { index ->
        val mod = Modifier.padding(index.dp)  // Creates new Modifier each time
    }
}
```

---

## 4. Dependency Injection

### 4.1 Hilt Setup

```kotlin
// ✅ GOOD - Use @HiltViewModel for ViewModels
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val clipRepository: ClipRepository,
    private val dispatchers: CoroutineDispatchers
) : BaseViewModel<UiState, UiAction, UiEffect>(initialState) {
    // ...
}

// ✅ GOOD - Use @Module and @InstallIn for providing dependencies
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideClipDatabase(
        @ApplicationContext context: Context
    ): ClipDatabase {
        return Room.databaseBuilder(context, ClipDatabase::class.java, "clips.db")
            .build()
    }
    
    @Provides
    @Singleton
    fun provideClipDao(db: ClipDatabase): ClipDao {
        return db.clipDao()
    }
}

// ❌ BAD - Don't use @AndroidEntryPoint (deprecated)
@AndroidEntryPoint
class MyActivity : AppCompatActivity() { /* ... */ }
```

### 4.2 Constructor Injection

```kotlin
// ✅ GOOD - Inject dependencies through constructor
class ClipRepository @Inject constructor(
    private val clipDao: ClipDao,
    private val dispatchers: CoroutineDispatchers
) {
    // ...
}

// ❌ BAD - Don't use field injection
class ClipRepository {
    @Inject lateinit var clipDao: ClipDao  // Wrong!
}

// ❌ BAD - Don't use @Inject on methods (unless for testing)
class ClipRepository @Inject constructor() {
    @Inject fun setDao(dao: ClipDao) { /* ... */ }  // Wrong!
}
```

### 4.3 Scope Management

```kotlin
// ✅ GOOD - Use appropriate scopes
@Singleton        // App lifetime
@ViewModelScoped  // ViewModel lifetime
@ActivityScoped   // Activity lifetime

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton  // Network client lives for app lifetime
    fun provideHttpClient(): OkHttpClient = OkHttpClient()
}

// ❌ BAD - Don't use @Singleton for UI-related objects
@Singleton
fun provideViewModel(): ClipViewModel = ClipViewModel()  // Wrong!
```

### 4.4 Qualifiers

```kotlin
// ✅ GOOD - Use @Named or custom qualifiers for multiple instances
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    @Provides
    @Singleton
    @Named("io")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @Singleton
    @Named("default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

// Usage
class MyRepository @Inject constructor(
    @Named("io") private val ioDispatcher: CoroutineDispatcher
)
```

---

## 5. Testing Standards

### 5.1 Test Structure

```kotlin
// ✅ GOOD - Use descriptive test names with backticks
class ClipViewModelTest {
    
    @Test
    fun `select clip updates selectedClip state`() = runTest {
        // Given
        val viewModel = ClipViewModel(testDao)
        val clip = ClipEntity("1", "Test content")
        
        // When
        viewModel.onAction(ClipUiAction.SelectClip(clip))
        
        // Then
        assertEquals(clip, viewModel.uiState.value.selectedClip)
    }
    
    @Test
    fun `apply transformation emits CopyToClipboard effect`() = runTest {
        // Given
        val viewModel = ClipViewModel(testDao)
        val clip = ClipEntity("1", "hello")
        
        // When
        viewModel.onAction(ClipUiAction.SelectClip(clip))
        viewModel.onAction(ClipUiAction.ApplyTransformation(UPPER_CASE))
        
        // Then
        viewModel.effect.test {
            val effect = awaitItem() as ClipUiEffect.CopyToClipboard
            assertEquals("HELLO", effect.text)
            awaitComplete()
        }
    }
}
```

### 5.2 Coroutine Testing

```kotlin
// ✅ GOOD - Use runTest with proper dispatcher setup
@OptIn(ExperimentalCoroutinesApi::class)
class ClipViewModelTest {
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `timer increments every second`() = runTest {
        val viewModel = ClipViewModel(testDao)
        
        viewModel.onAction(ClipUiAction.ToggleTimer)
        advanceUntilIdle()
        
        assertEquals(0, viewModel.uiState.value.timerSeconds)
        
        delay(1000L)
        assertEquals(1, viewModel.uiState.value.timerSeconds)
    }
}

// ❌ BAD - Don't use runBlocking in tests
@Test
fun badTest() = runBlocking {  // Wrong!
    // Blocks the test thread
}
```

### 5.3 Effect Testing with Turbine

```kotlin
// ✅ GOOD - Consume all effects in order
@Test
fun `transformation emits two effects`() = runTest {
    val viewModel = ClipViewModel(testDao)
    
    viewModel.effect.test {
        viewModel.onAction(ClipUiAction.ApplyTransformation(UPPER_CASE))
        
        // First effect
        val copyEffect = awaitItem() as ClipUiEffect.CopyToClipboard
        assertEquals("HELLO", copyEffect.text)
        
        // Second effect
        val toastEffect = awaitItem() as ClipUiEffect.ShowToast
        assertTrue(toastEffect.message.contains("Transformation"))
        
        // Ensure no more effects
        awaitComplete()
    }
}

// ❌ BAD - Don't leave effects unconsumed
@Test
fun badTest() = runTest {
    val viewModel = ClipViewModel(testDao)
    
    viewModel.effect.test {
        viewModel.onAction(ClipUiAction.ApplyTransformation(UPPER_CASE))
        awaitItem()
        // Missing: awaitComplete() or consuming remaining effects
    }  // Fails with "Unconsumed events found"
}
```

### 5.4 Repository Testing

```kotlin
// ✅ GOOD - Mock dependencies, test behavior
class ClipRepositoryTest {
    
    private val mockDao = mockk<ClipDao>()
    private val repository = ClipRepository(mockDao, Dispatchers.Main)
    
    @Test
    fun `getClips returns flow of clips`() = runTest {
        // Given
        val testClips = listOf(ClipEntity("1", "Test"))
        every { mockDao.getAllClips() } returns flowOf(testClips)
        
        // When
        val result = repository.getClips().first()
        
        // Then
        assertEquals(testClips, result)
        verify { mockDao.getAllClips() }
    }
}
```

### 5.5 Test Coverage Requirements

| Component | Minimum Coverage | Testing Approach |
|-----------|-----------------|------------------|
| ViewModels | 80% | State transitions, effects, actions |
| UseCases | 90% | All code paths, edge cases |
| Repositories | 80% | DAO interactions, error handling |
| DAOs | 70% | CRUD operations |
| Utilities | 90% | All transformations |
| Composables | N/A | Manual testing, screenshot tests |

---

## 6. Error Handling

### 6.1 Sealed Error Types

```kotlin
// ✅ GOOD - Use sealed classes for error types
sealed interface AppError {
    data class Network(val statusCode: Int, val message: String) : AppError
    data class Database(val exception: Exception) : AppError
    data class Validation(val field: String, val message: String) : AppError
    data object Unauthorized : AppError
    data object NotFound : AppError
}

// Usage
sealed interface UiState {
    data object Loading : UiState
    data class Success<T>(val data: T) : UiState
    data class Error(val error: AppError) : UiState
}
```

### 6.2 Result Type Pattern

```kotlin
// ✅ GOOD - Use Result for fallible operations
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

// Usage
class ClipRepository @Inject constructor(
    private val clipDao: ClipDao
) {
    suspend fun insertClip(clip: ClipEntity): Result<Unit> {
        return try {
            clipDao.insertClip(clip)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### 6.3 Exception Handling in ViewModels

```kotlin
// ✅ GOOD - Catch exceptions and send as effects
@HiltViewModel
class ClipViewModel @Inject constructor(
    private val repository: ClipRepository
) : BaseViewModel<UiState, UiAction, UiEffect>(initialState) {
    
    override fun onAction(action: UiAction) {
        when (action) {
            is UiAction.SaveClip -> {
                viewModelScope.launch {
                    when (val result = repository.saveClip(action.clip)) {
                        is Result.Success -> {
                            updateState { it.copy(saved = true) }
                        }
                        is Result.Error -> {
                            sendEffect(UiEffect.ShowError(result.exception.message))
                        }
                    }
                }
            }
        }
    }
}

// ❌ BAD - Don't let exceptions propagate to UI
override fun onAction(action: UiAction) {
    repository.saveClip(action.clip)  // Uncaught exception!
}
```

### 6.4 Logging

```kotlin
// ✅ GOOD - Use proper logging with TAG
private const val TAG = "ClipViewModel"

class ClipViewModel {
    
    fun onSaveClip(clip: ClipEntity) {
        Log.d(TAG, "Saving clip: ${clip.id}")
        // ...
    }
    
    fun onError(e: Exception) {
        Log.e(TAG, "Error saving clip", e)
    }
}

// ❌ BAD - Don't log sensitive data
Log.d(TAG, "User password: $password")  // Never!
Log.d(TAG, "API Key: $apiKey")          // Never!
```

---

## 7. Performance Considerations

### 7.1 Coroutine Scopes

```kotlin
// ✅ GOOD - Use viewModelScope for ViewModel operations
@HiltViewModel
class ClipViewModel @Inject constructor() : BaseViewModel(...) {
    
    override fun onAction(action: UiAction) {
        viewModelScope.launch {
            // Automatically cancelled when ViewModel is cleared
            repository.saveClip(action.clip)
        }
    }
}

// ✅ GOOD - Use rememberCoroutineScope for Composable side effects
@Composable
fun SaveButton(viewModel: ClipViewModel) {
    val scope = rememberCoroutineScope()
    
    Button(onClick = {
        scope.launch {
            viewModel.saveClip()
        }
    }) {
        Text("Save")
    }
}

// ❌ BAD - Don't use GlobalScope
GlobalScope.launch {  // Never!
    // Leaks memory
}
```

### 7.2 Flow Optimization

```kotlin
// ✅ GOOD - Use debounce for rapid updates
val searchQuery: StateFlow<String> = _searchQuery
    .debounce(300)
    .stateIn(viewModelScope, SharingStarted.Lazily, "")

// ✅ GOOD - Use distinctUntilChanged to avoid redundant emissions
val clips: StateFlow<List<Clip>> = _clips
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// ✅ GOOD - Use combine for multiple flows
val uiState = combine(
    clips,
    searchQuery,
    filterSettings
) { clips, query, filters ->
    ClipsUiState(clips, query, filters)
}.stateIn(viewModelScope, SharingStarted.Lazily, initial)
```

### 7.3 Memory-Efficient Composables

```kotlin
// ✅ GOOD - Use remember for expensive calculations
@Composable
fun ClipList(clips: List<Clip>) {
    val sortedClips = remember(clips) {
        clips.sortedByDescending { it.timestamp }
    }
    
    LazyColumn {
        items(sortedClips, key = { it.id }) { clip ->
            ClipItem(clip)
        }
    }
}

// ✅ GOOD - Use derivedStateOf for computed properties
@Composable
fun ClipStats(clips: List<Clip>) {
    val pinnedCount by remember(clips) {
        derivedStateOf { clips.count { it.isPinned } }
    }
    
    Text("Pinned: $pinnedCount")
}

// ❌ BAD - Don't create objects in the Composable body
@Composable
fun BadClipList(clips: List<Clip>) {
    val sorted = clips.sorted()  // Creates new list every recomposition!
    // ...
}
```

### 7.4 Image Loading

```kotlin
// ✅ GOOD - Use Coil with proper placeholders and caching
@Composable
fun ClipIcon(url: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = "Clip icon",
        modifier = Modifier.size(48.dp),
        placeholder = painterResource(R.drawable.ic_placeholder),
        error = painterResource(R.drawable.ic_error)
    )
}

// ✅ GOOD - Use memory cache for frequently accessed images
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25)
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(cacheDir)
            .maxSizeBytes(100 * 1024 * 1024)  // 100MB
            .build()
    }
    .build()
```

---

## 8. Security Best Practices

### 8.1 Encryption at Rest

```kotlin
// ✅ GOOD - Use SQLCipher for database encryption
val factory = SupportFactory(
    cryptoManager.getOrCreateKey().encoded
)

val database = Room.databaseBuilder(context, MyDatabase::class.java, "encrypted.db")
    .openHelperFactory(factory)
    .build()

// ✅ GOOD - Use Android Keystore for key management
class CryptoManager @Inject constructor() {
    
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        
        return (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: createKey(keyStore)
    }
    
    private fun createKey(keyStore: KeyStore): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)  // Set true for biometric
            .build()
        
        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        ).init(spec).generateKey()
    }
}
```

### 8.2 Secure Data Storage

```kotlin
// ✅ GOOD - Use EncryptedSharedPreferences for sensitive data
val masterKey = MasterKey.Builder(context)
    .setMasterKeyAlg(MasterKey.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// ✅ GOOD - Never store sensitive data in regular SharedPreferences
val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
prefs.edit().putString("password", password).apply()  // NEVER!
```

### 8.3 Network Security

```kotlin
// ✅ GOOD - Use HTTPS only
val client = OkHttpClient.Builder()
    .sslSocketFactory(
        context.sslContext.socketFactory,
        context.trustManager
    )
    .certificatePinner(
        CertificatePinner.Builder()
            .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    )
    .build()

// ❌ BAD - Never disable SSL verification
OkHttpClient.Builder()
    .sslSocketFactory(insecureSslSocketFactory, insecureTrustManager)  // NEVER!
    .build()
```

### 8.4 Input Validation

```kotlin
// ✅ GOOD - Validate all user inputs
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

fun validateEmail(email: String): ValidationResult {
    return if (email.isBlank()) {
        ValidationResult.Invalid("Email is required")
    } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        ValidationResult.Invalid("Invalid email format")
    } else {
        ValidationResult.Valid
    }
}

fun validateClipContent(content: String): ValidationResult {
    return when {
        content.isBlank() -> ValidationResult.Invalid("Content cannot be empty")
        content.length > MAX_CONTENT_LENGTH -> 
            ValidationResult.Invalid("Content too long (max $MAX_CONTENT_LENGTH chars)")
        content.contains("<script>") -> 
            ValidationResult.Invalid("Script tags not allowed")
        else -> ValidationResult.Valid
    }
}
```

### 8.5 Obfuscation

```gradle
// ✅ GOOD - Enable ProGuard/R8 for release builds
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 9. Memory Management

### 9.1 Lifecycle-Aware Resources

```kotlin
// ✅ GOOD - Use lifecycleScope in Activities/Fragments
class ClipActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            // Automatically cancelled when Activity is destroyed
            viewModel.clips.collect { clips ->
                updateUI(clips)
            }
        }
    }
}

// ✅ GOOD - Use DisposableEffect in Composables
@Composable
fun Timer(viewModel: ClipViewModel) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTimer()  // Cleanup when Composable leaves composition
        }
    }
}

// ❌ BAD - Don't leak Context
class MyRepository {
    constructor(context: Context) {  // Wrong! Context can leak
        // Use ApplicationContext instead
    }
}
```

### 9.2 Flow Cancellation

```kotlin
// ✅ GOOD - Use viewModelScope for automatic cancellation
@HiltViewModel
class ClipViewModel @Inject constructor() : BaseViewModel(...) {
    
    init {
        viewModelScope.launch {
            // Cancelled when ViewModel is cleared
            repository.clips.collect { updateUI(it) }
        }
    }
}

// ✅ GOOD - Use cancel on error
viewModelScope.launch {
    try {
        repository.fetchData().collect { /* ... */ }
    } catch (e: Exception) {
        // Coroutine is automatically cancelled on exception
        handleError(e)
    }
}

// ❌ BAD - Don't use unconfined dispatcher for long-running tasks
lifecycleScope.launch(Dispatchers.Unconfined) {  // Wrong!
    // Not tied to lifecycle
}
```

### 9.3 Bitmap Management

```kotlin
// ✅ GOOD - Use ImageRequest with proper sizing
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .size(Size(200, 200))  // Limit memory usage
        .crossfade(true)
        .build(),
    contentDescription = null
)

// ✅ GOOD - Recycle bitmaps when not using ImageLoader
@Composable
fun CustomImage(bitmap: Bitmap) {
    DisposableEffect(bitmap) {
        onDispose {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
    Image(bitmap = bitmap, contentDescription = null)
}
```

### 9.4 Leak Detection

```kotlin
// ✅ GOOD - Use LeakCanary in debug builds
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}

// ✅ GOOD - Check for common leak patterns
// - Static Context references
// - Non-static inner classes holding Activity references
// - Unclosed Cursors, Streams, Connections
// - Unregistered Broadcast Receivers
// - Leaked Handlers
```

---

## 10. Documentation Standards

### 10.1 KDoc Requirements

```kotlin
// ✅ GOOD - Document public APIs with KDoc
/**
 * Repository for managing clipboard clips.
 *
 * Provides CRUD operations for [ClipEntity] objects with support for
 * encrypted storage and search functionality.
 *
 * @property clipDao The DAO for database operations
 * @property dispatchers Coroutine dispatchers for background operations
 */
class ClipRepository @Inject constructor(
    private val clipDao: ClipDao,
    private val dispatchers: CoroutineDispatchers
) {
    
    /**
     * Retrieves all clips from the database.
     *
     * @return A [Flow] emitting the list of clips. Emits an empty list if no clips exist.
     */
    fun getAllClips(): Flow<List<ClipEntity>> {
        return clipDao.getAllClips()
    }
    
    /**
     * Searches clips by text content.
     *
     * @param query The search query (case-insensitive)
     * @return A [Flow] emitting matching clips
     */
    fun searchClips(query: String): Flow<List<ClipEntity>> {
        return clipDao.searchClips("%$query%")
    }
}
```

### 10.2 Function Documentation

```kotlin
// ✅ GOOD - Document complex logic and edge cases
/**
 * Applies text transformation to the input string.
 *
 * Handles the following transformation types:
 * - [TransformationType.UPPER_CASE]: Converts all characters to uppercase
 * - [TransformationType.LOWER_CASE]: Converts all characters to lowercase
 * - [TransformationType.JSON_PRETTY]: Formats JSON with indentation
 *
 * @param text The input text to transform
 * @param type The transformation type to apply
 * @return The transformed text
 *
 * @throws IllegalArgumentException if [text] is null or [type] is unsupported
 */
fun transformText(text: String, type: TransformationType): String {
    require(text.isNotBlank()) { "Text cannot be empty" }
    
    return when (type) {
        TransformationType.UPPER_CASE -> text.uppercase()
        TransformationType.LOWER_CASE -> text.lowercase()
        // ...
    }
}
```

### 10.3 README Files

Each module must have a README.md with:

```markdown
# Module Name

## Purpose
Brief description of what this module does.

## Dependencies
- List of key dependencies modules

## Key Components
- `ViewModelName`: Description
- `RepositoryName`: Description
- `EntityName`: Description

## Usage Example
```kotlin
// Example code showing how to use this module
```

## Testing
- Unit tests: [Link to test directory]
- Coverage: XX%
```

### 10.4 Commit Messages

```
# ✅ GOOD - Conventional commits
feat(clip): Add search functionality to clipboard manager
fix(scratch): Resolve timer not stopping on ViewModel clear
refactor(datastore): Extract encryption logic to CryptoManager
test(clip): Add unit tests for ClipViewModel state transitions
docs(readme): Update setup instructions for new dependencies

# ❌ BAD - Vague commit messages
fix bug
update code
add feature
```

---

## Quick Reference Checklist

Before submitting any code change:

### Architecture
- [ ] Follows Clean Architecture layers
- [ ] Single responsibility principle
- [ ] Proper package structure
- [ ] No circular dependencies

### Code Quality
- [ ] Null safety used consistently
- [ ] Immutability preferred over mutability
- [ ] Type inference used appropriately
- [ ] No code duplication (DRY)

### Compose
- [ ] State hoisted to parent
- [ ] Stable types used
- [ ] Keys provided for LazyList items
- [ ] No state created in loops
- [ ] Side effects using LaunchedEffect/DisposableEffect

### Testing
- [ ] ViewModels have state transition tests
- [ ] All effects are consumed in tests
- [ ] Coroutine dispatchers mocked
- [ ] Test names are descriptive

### Security
- [ ] No sensitive data in logs
- [ ] Encryption used for sensitive data
- [ ] Input validation implemented
- [ ] HTTPS only for network calls

### Performance
- [ ] No GlobalScope usage
- [ ] Flows properly cancelled
- [ ] Expensive operations cached with remember
- [ ] Image loading uses caching

### Documentation
- [ ] Public APIs have KDoc
- [ ] Complex logic explained
- [ ] Commit message follows convention
- [ ] README updated if needed

---

## Enforcement

These best practices are enforced through:

1. **Code Review**: All PRs reviewed against this document
2. **Lint Rules**: Custom lint rules for common violations
3. **Unit Tests**: Test coverage requirements must be met
4. **CI/CD**: Automated checks for build and test success

Violations must be justified with a comment explaining why the exception is necessary.

---

## 11. Generated Documentation Management

### Markdown File Storage Policy

All non-documentation `.md` files generated during development must follow this storage and lifecycle policy.

#### Storage Location

Generated markdown files MUST be stored in `.md-storage/` directory:

```
.md-storage/
├── audit-reports/          # Code audit and antipattern reports
├── analysis/               # Code analysis and exploration results
├── planning/               # Work plans and task tracking
├── summaries/              # Quality summaries and metrics
├── temporary/              # Temporary working documents
└── archived/               # Completed/stale documents
```

#### File Categories

| Category | Location | Lifetime |
|----------|----------|----------|
| **Permanent Documentation** | Root or `/docs/` | Indefinite |
| **Audit Reports** | `.md-storage/audit-reports/` | 6 months |
| **Analysis Results** | `.md-storage/analysis/` | 3 months |
| **Work Plans** | `.md-storage/planning/` | Project end |
| **Summaries** | `.md-storage/summaries/` | 3 months |
| **Temporary** | `.md-storage/temporary/` | 2 weeks |

#### Lifecycle Rules

1. **Fresh** (0-30 days): Active use, may be referenced
2. **Review** (30-90 days): Verify still relevant
3. **Stale** (90+ days): Archive or delete
4. **Expired** (180+ days): Delete unless justified

#### Required Actions

**When creating a generated `.md` file:**
1. Determine category and lifetime
2. Store in appropriate `.md-storage/` subdirectory
3. Add creation date to filename: `YYYY-MM-DD-description.md`
4. Create symlink in root ONLY if actively referenced

**When reviewing code:**
1. Check `.md-storage/` for stale files (>90 days)
2. Archive or delete expired files
3. Update symlinks if documents moved

**Before merge:**
1. Ensure no temporary files in root directory
2. Verify all generated docs are in `.md-storage/`
3. Run cleanup script: `./scripts/cleanup-md-storage.sh`

#### Examples

```bash
# ✅ CORRECT - Generated report in storage
.md-storage/audit-reports/2024-09-24-antipatterns-audit.md

# ✅ CORRECT - Active document with symlink
.md-storage/planning/2024-09-24-work-plan.md -> WORK_PLAN.md

# ❌ WRONG - Generated file in root (unless permanent)
ANTIPATTERNS_REPORT.md  # Should be in .md-storage/

# ❌ WRONG - Temporary file in root
TEMP_ANALYSIS.md  # Should be in .md-storage/temporary/
```

#### Automated Cleanup

Run cleanup script monthly:
```bash
./scripts/cleanup-md-storage.sh
```

This script will:
- Archive files older than 90 days
- Delete files older than 180 days
- Report actions taken
- Require confirmation for deletions

---

## 12. Third-Party Dependency License Requirements

### License Policy

**ALL third-party packages, SDKs, libraries, and binaries used in this project MUST be under a FULLY PERMISSIVE license.**

#### Approved Licenses

Only the following licenses are permitted:

| License | Status | Notes |
|---------|--------|-------|
| **MIT** | ✅ Approved | Fully permissive |
| **Apache 2.0** | ✅ Approved | Fully permissive, patent grant |
| **BSD 2-Clause** | ✅ Approved | Fully permissive |
| **BSD 3-Clause** | ✅ Approved | Fully permissive |
| **ISC** | ✅ Approved | Fully permissive |
| **CC0-1.0** | ✅ Approved | Public domain |
| **Unlicense** | ✅ Approved | Public domain |
| **EPL 2.0** | ⚠️ Conditional | Requires review |
| **LGPL 2.1+** | ⚠️ Conditional | Requires review, dynamic linking only |
| **GPL 2.0/3.0** | ❌ PROHIBITED | Copyleft license |
| **AGPL 3.0** | ❌ PROHIBITED | Strong copyleft |
| **MPL 2.0** | ⚠️ Conditional | Requires review |
| **Proprietary** | ❌ PROHIBITED | No commercial licenses |
| **Unknown** | ❌ PROHIBITED | Must verify before use |

#### Definition: Fully Permissive

A license is considered "fully permissive" if it:
- ✅ Allows commercial use
- ✅ Allows modification
- ✅ Allows distribution
- ✅ Allows private use
- ✅ Requires only attribution (not derivative works to be open-source)
- ✅ Has no copyleft provisions
- ✅ Has no field-of-use restrictions

#### Prohibited License Types

The following license types are **STRICTLY PROHIBITED**:

1. **Copyleft Licenses** (GPL, AGPL, LGPL)
   - Require derivative works to be open-source
   - Create legal liability for proprietary code

2. **Proprietary/Commercial Licenses**
   - Require payment for production use
   - Have field-of-use restrictions
   - Limit redistribution rights

3. **Weak/Permissive Licenses** (MPL, EPL)
   - May require source disclosure for modifications
   - Require legal review before use

4. **Unknown/Unclear Licenses**
   - No license file present
   - Ambiguous license terms
   - Conflicting license claims

### Dependency Management

#### Adding New Dependencies

Before adding any third-party dependency:

1. **Verify License:**
   ```bash
   # Check license in repository
   curl -s https://raw.githubusercontent.com/owner/repo/main/LICENSE
   
   # Use license checker tool
   ./scripts/check-licenses.sh --verify <dependency>
   ```

2. **Document Justification:**
   - Why this dependency is needed
   - Why alternatives were rejected
   - License verification result

3. **Update License Report:**
   - Add to `third-party-licenses.md`
   - Include license text if required

#### Required Documentation

Create/update `third-party-licenses.md` in root:

```markdown
# Third-Party License Report

## Approved Dependencies

| Dependency | Version | License | Verified |
|------------|---------|---------|----------|
| kotlinx-coroutines | 1.7.0 | Apache 2.0 | ✅ 2024-09-24 |
| compose-bom | 2024.02.00 | Apache 2.0 | ✅ 2024-09-24 |
| hilt | 2.60.1 | Apache 2.0 | ✅ 2024-09-24 |

## Prohibited Dependencies (None)

No dependencies with prohibited licenses are used.

## Review Process

All dependencies reviewed by:
- [ ] License verification
- [ ] Security vulnerability scan
- [ ] Maintenance activity check
- [ ] Alternative evaluation
```

### Enforcement

#### Automated Checks

1. **Pre-commit Hook:**
   - Scans `build.gradle.kts` for new dependencies
   - Flags unverified licenses
   - Requires justification comment

2. **CI/CD Pipeline:**
   - Runs license verification on all PRs
   - Fails build on prohibited licenses
   - Generates license report

3. **Gradle Plugin:**
   - Use `gradle-license-plugin` or similar
   - Configure allowed license patterns
   - Fail build on violations

#### Configuration Example

```kotlin
// gradle/license-config.gradle.kts
licenseChecks {
    allowedLicenses = listOf(
        "MIT",
        "Apache 2.0",
        "BSD-2-Clause",
        "BSD-3-Clause",
        "ISC",
        "CC0-1.0"
    )
    
    prohibitedLicenses = listOf(
        "GPL-2.0",
        "GPL-3.0",
        "AGPL-3.0",
        "Proprietary"
    )
    
    conditionalLicenses = listOf(
        "LGPL-2.1",
        "MPL-2.0",
        "EPL-2.0"
    )
    
    // Require justification for conditional licenses
    requireJustification = true
}
```

### Violation Handling

#### Detected Violations

If a prohibited license is detected:

1. **Immediate Action:**
   - Remove dependency from build files
   - Document the violation
   - Notify team lead

2. **Remediation:**
   - Find alternative with approved license
   - Evaluate fork with re-licensed code (if possible)
   - Implement feature without dependency

3. **Prevention:**
   - Update dependency check configuration
   - Add to team training
   - Review similar dependencies

#### Exception Process

Rare cases may require exceptions:

1. **Submit Request:**
   - Dependency name and version
   - License type and text
   - Business justification
   - Risk assessment
   - Alternative evaluation

2. **Review Board:**
   - Legal review
   - Security review
   - Architecture review

3. **Decision:**
   - Approved with conditions
   - Denied with alternatives
   - Escalated to leadership

### Maintenance

#### Quarterly Review

Every quarter:
- Audit all dependencies
- Verify licenses still valid
- Check for license changes in updates
- Update `third-party-licenses.md`

#### Security Scanning

Integrate with security tools:
- `OWASP Dependency-Check`
- `Snyk`
- `GitHub Dependabot`
- `Gradle License Checker`

### References

- [SPDX License List](https://spdx.org/licenses/)
- [Open Source Initiative](https://opensource.org/licenses)
- [FOSSA License Guide](https://fossa.com/blog/open-source-software-licenses-101/)
- [Choose a License](https://choosealicense.com/)
