# Core Data Module

Centralized data layer infrastructure providing repository patterns, data sources, and caching mechanisms.

## Overview

The `core:data` module establishes the foundation for data management across the Alloy application. It implements clean architecture principles with clear separation between:

- **Domain layer** (via `core:domain`): Business logic and use cases
- **Data layer**: Repository implementations and data sources
- **Infrastructure**: Caching, serialization, and external integrations

## Architecture

```
core/data/
├── build.gradle.kts
├── src/
│   └── main/
│       └── java/com/squidink/alloy/core/data/
│           ├── cache/
│           │   ├── CachePolicy.kt           # Cache strategy definitions
│           │   └── MemoryCache.kt           # In-memory caching implementation
│           ├── datasource/
│           │   ├── LocalDataSource.kt       # Local data source interface
│           │   ├── RemoteDataSource.kt      # Remote data source interface
│           │   └── StatsDataSources.kt      # System stats implementations
│           ├── di/
│           │   └── DataModule.kt            # Hilt DI module
│           ├── mapper/
│           │   └── Mapper.kt                # Data transformation interfaces
│           └── repository/
│               ├── BaseRepository.kt        # Base repository implementation
│               └── SettingsRepository.kt    # Settings persistence repository
└── README.md
```

## Components

### Cache Layer

#### CachePolicy

Defines data retrieval strategies:

```kotlin
enum class CachePolicy {
    CACHE_FIRST,      // Check cache first, fallback to remote
    NETWORK_FIRST,    // Try remote first, fallback to cache
    CACHE_ONLY,       // Use cache exclusively
    NETWORK_ONLY      // Use remote exclusively
}
```

**Usage:**
```kotlin
val policy = CachePolicy.CACHE_FIRST
if (policy.shouldUseCache()) {
    // Read from cache
}
if (policy.shouldUseRemote()) {
    // Fetch from remote
}
```

#### MemoryCache

Thread-safe in-memory cache with TTL support:

```kotlin
val cache = MemoryCache<String, User>(
    maxSize = 100,
    defaultTtlSeconds = 300
)

// Store value
cache.put("user_123", user)

// Retrieve value
val user = cache.get("user_123")

// Check existence
if (cache.contains("user_123")) { /* ... */ }

// Remove entry
cache.remove("user_123")

// Clear all
cache.clear()
```

**Features:**
- LRU eviction when capacity exceeded
- Automatic expiration based on TTL
- Thread-safe operations
- Null-safe get operations

### Data Sources

#### LocalDataSource Interface

Interface for local data operations (Room, DataStore, files):

```kotlin
interface LocalDataSource<Data, ID> {
    suspend fun getById(id: ID): Data?
    suspend fun getAll(): List<Data>
    suspend fun insert(item: Data): ID
    suspend fun update(item: Data)
    suspend fun delete(id: ID)
    fun observeById(id: ID): Flow<Data?>
    fun observeAll(): Flow<List<Data>>
}
```

#### RemoteDataSource Interface

Interface for remote data operations (API, system services):

```kotlin
interface RemoteDataSource<Data, ID> {
    suspend fun fetchById(id: ID): Data?
    suspend fun fetchAll(): List<Data>
    suspend fun create(item: Data): ID
    suspend fun update(item: Data)
    suspend fun delete(id: ID)
}
```

#### SystemStatsDataSource

Provides system statistics (CPU, memory, network):

```kotlin
@Singleton
class SystemStatsDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader,
    private val cache: MemoryCache<String, Any>
) : LocalDataSource<SystemStatsData, String>
```

**Usage:**
```kotlin
// Observe current stats
val statsFlow: Flow<SystemStatsData> = dataSource.currentStats

// Poll immediately
val stats = dataSource.pollStats()
```

**SystemStatsData Model:**
```kotlin
data class SystemStatsData(
    val memoryUsedBytes: Long = 0,
    val memoryTotalBytes: Long = 0,
    val memoryPercent: Float = 0f,
    val cpuPercent: Float = 0f,
    val netStats: NetStats = NetStats(),
    val timestamp: Long = System.currentTimeMillis()
)
```

**NetStats Domain Model:**

Network statistics are also defined in `core:domain` for domain independence:

```kotlin
// core/domain/src/main/java/com/squidink/alloy/core/domain/repository/DomainModels.kt
data class NetStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxBytesPerSecond: Float = 0f,
    val txBytesPerSecond: Float = 0f,
)
```

The `SystemStatsDataSource` provides access to network stats through the embedded `NetStats`:

```kotlin
// core/data/src/main/java/com/squidink/alloy/core/data/datasource/StatsDataSources.kt
class SystemStatsDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader,
    private val cache: MemoryCache<String, Any>
) {
    val currentStats: Flow<SystemStatsData>
}
```

#### BatteryDataSource

Provides battery information from Android system:

```kotlin
@Singleton
class BatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : RemoteDataSource<BatteryInfo, String>
```

**Usage:**
```kotlin
// Register for real-time updates
val receiver = dataSource.registerBatteryReceiver()

// Observe battery changes
val batteryFlow: Flow<BatteryInfo> = dataSource.batteryInfo

// Get current state
val batteryInfo = dataSource.getCurrentBatteryInfo()

// Unregister when done
dataSource.unregisterBatteryReceiver(receiver)
```

**BatteryInfo Model:**

The `BatteryInfo` domain model is defined in `core:domain` for business logic independence:

```kotlin
// core/domain/src/main/java/com/squidink/alloy/core/domain/repository/DomainModels.kt
data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = 0,
    val status: Int = 0,
    val temperature: Int = 0, // tenths of a degree Celsius
    val voltage: Int = 0, // millivolts
    val isCharging: Boolean = false,
) {
    fun getTemperatureCelsius(): Float = temperature / 10f
}
```

The data layer (`core:data`) provides `BatteryDataSource` which emits this domain model:

```kotlin
// core/data/src/main/java/com/squidink/alloy/core/data/datasource/BatteryDataSource.kt
class BatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val batteryInfo: Flow<BatteryInfo> // Emits domain model
}
```

### Repositories

#### BaseRepository

Abstract repository implementing common CRUD operations with local/remote coordination:

```kotlin
abstract class BaseRepository<Domain, Data, ID>(
    protected val localDataSource: LocalDataSource<Data, ID>,
    protected val remoteDataSource: RemoteDataSource<Data, ID>? = null
)
```

**Available Methods:**
- `observeAll(): Flow<List<Domain>>` - Observe all items
- `observeById(id: ID): Flow<Domain?>` - Observe single item
- `getAll(): List<Domain>` - Get all items
- `getById(id: ID): Domain?` - Get single item
- `insert(domain: Domain): ID` - Insert new item
- `update(domain: Domain)` - Update existing item
- `delete(id: ID)` - Delete item
- `refreshFromRemote()` - Refresh from remote source
- `refreshById(id: ID)` - Refresh single item
- `fetchWithPolicy(policy: CachePolicy): Flow<Domain?>` - Fetch with cache strategy

**Usage:**
```kotlin
class StatsRepository(
    localDataSource: LocalDataSource<StatsData, String>,
    remoteDataSource: RemoteDataSource<StatsData, String>?
) : BaseRepository<Stats, StatsData, String>(localDataSource, remoteDataSource) {
    
    override fun getId(data: StatsData) = "current"
    override fun getIdFromDomain() = "current"
    override fun mapToDomain(data: StatsData) = Stats(...)
    override fun mapToData(domain: Stats) = StatsData(...)
}
```

#### MappedRepository

Repository with explicit mapper support for cleaner transformation logic:

```kotlin
abstract class MappedRepository<Domain, Data, ID>(
    localDataSource: LocalDataSource<Data, ID>,
    remoteDataSource: RemoteDataSource<Data, ID>? = null,
    private val toDomainMapper: DataMapper<Data, Domain>,
    private val toDataMapper: DataMapper<Domain, Data>
) : BaseRepository<Domain, Data, ID>
```

**Usage:**
```kotlin
class UserRepository(
    localDataSource: LocalDataSource<UserData, Long>,
    toDomain: DataMapper<UserData, User>,
    toData: DataMapper<User, UserData>
) : MappedRepository<User, UserData, Long>(
    localDataSource, null, toDomain, toData
)
```

#### SettingsRepository

Manages application settings persistence:

```kotlin
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStoreManager: DataStoreManager
)
```

**Settings Managed:**
- `showPill`: Show/hide stats pill overlay
- `usePercentages`: Display percentages vs raw values
- `cornerPosition`: Overlay position (TOP_LEFT, TOP_RIGHT, etc.)

**Usage:**
```kotlin
// Observe settings
val showPillFlow: Flow<Boolean> = repository.observeShowPill()
val usePercentagesFlow: Flow<Boolean> = repository.observeUsePercentages()
val cornerPositionFlow: Flow<String> = repository.observeCornerPosition()

// Update settings
repository.setShowPill(true)
repository.setUsePercentages(false)
repository.setCornerPosition("TOP_RIGHT")
```

### Mappers

#### DataMapper Interface

Functional interface for data transformation:

```kotlin
interface DataMapper<In, Out> {
    fun map(input: In): Out
}
```

**Usage:**
```kotlin
val userMapper = DataMapper<UserData, User> { userData ->
    User(
        id = userData.id,
        name = userData.name,
        email = userData.email
    )
}
```

## Dependency Injection

### DataModule

Hilt module providing data layer dependencies:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindDataStoreManager(
        impl: DataStoreManager
    ): IDataStoreManager
}
```

## Dependencies

```kotlin
implementation(project(":core:common"))
implementation(project(":core:domain"))
implementation(project(":core:design"))

// AndroidX
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.lifecycle.runtime.ktx)

// Kotlin
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.core)

// Hilt
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
```

## Testing

### Unit Tests

```kotlin
// Test MemoryCache
@Test
fun `cache put and get`() {
    val cache = MemoryCache<String, String>()
    cache.put("key", "value")
    assertEquals("value", cache.get("key"))
}

// Test Repository
@Test
fun `repository inserts data`() = runTest {
    val mockLocal = mockk<LocalDataSource<Data, String>>()
    val repository = TestRepository(mockLocal)
    
    val result = repository.insert(TestDomain())
    verify { mockLocal.insert(any()) }
}
```

### Fake Implementations

```kotlin
class FakeLocalDataSource : LocalDataSource<Data, String> {
    private val items = mutableMapOf<String, Data>()
    
    override suspend fun getById(id: String) = items[id]
    override suspend fun getAll() = items.values.toList()
    // ... implement other methods
}
```

## Best Practices

1. **Use Flows for reactive data**: Always expose `Flow<T>` for observable data
2. **Cache strategically**: Use `MemoryCache` for frequently accessed, expensive data
3. **Repository pattern**: Keep data source details hidden from use cases
4. **Mapper separation**: Use `DataMapper` for clear transformation boundaries
5. **Error handling**: Wrap data operations in try-catch and log errors
6. **Thread dispatching**: Use `flowOn(Dispatchers.IO)` for data operations

## Future Enhancements

- [ ] Room database integration
- [ ] Network status monitoring
- [ ] Offline sync queue
- [ ] Data encryption at rest
- [ ] Pagination support
