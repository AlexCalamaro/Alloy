# ADR-001: Database Factory Initialization Pattern

**Status:** Accepted  
**Date:** 2024-09-24  
**Authors:** Architecture Team  
**Context:** EncryptedRoomFactory thread-safety issue

---

## Context

The `EncryptedRoomFactory.getFactoryFor()` method was originally implemented as a synchronous function that internally used `runBlocking` to perform I/O and cryptographic operations. This pattern has several issues:

1. **Thread Safety Violation**: `runBlocking` inside a synchronous method can cause ANR if called on the main thread
2. **Incorrect Coroutine Usage**: Calling suspend functions from within `runBlocking` without proper dispatcher context
3. **Misleading API**: The method signature doesn't indicate it performs blocking I/O operations

```kotlin
// PROBLEMATIC PATTERN
fun getFactoryFor(dbName: String): SupportFactory {
    val passphraseBytes = runBlocking {  // ❌ Blocks thread
        dataStoreManager.setString(...)   // ❌ Suspend in runBlocking
        // ...
    }
    return SupportFactory(passphraseBytes)
}
```

## Decision

We will use a **suspend function** with explicit dispatcher context:

```kotlin
// CORRECT PATTERN
suspend fun getFactoryFor(dbName: String): SupportFactory {
    return withContext(Dispatchers.IO) {
        val passphraseBytes = dataStoreManager.getStringFlow(...).first()
        // ... I/O operations
        SupportFactory(passphraseBytes)
    }
}
```

For Hilt `@Provides` methods (which cannot be suspend), we use **runBlocking with explicit dispatcher**:

```kotlin
// HILT PROVIDER PATTERN
@Provides
@Singleton
fun provideClipDatabase(
    @ApplicationContext context: Context,
    encryptedRoomFactory: EncryptedRoomFactory
): ClipDatabase {
    // Safe: runBlocking with Dispatchers.IO runs on background thread
    val factory = runBlocking(Dispatchers.IO) {
        encryptedRoomFactory.getFactoryFor("alloy_clip")
    }
    
    return Room.databaseBuilder(...)
        .openHelperFactory(factory)
        .build()
}
```

## Consequences

### Positive
- ✅ Clear API contract indicating async/I/O operations
- ✅ Prevents accidental main thread blocking
- ✅ Proper coroutine scope and dispatcher usage
- ✅ Better testability (can use TestCoroutineDispatcher)
- ✅ Consistent with Kotlin coroutine best practices

### Negative
- ⚠️ Requires callers to handle suspend function
- ⚠️ Hilt providers need runBlocking wrapper
- ⚠️ Slightly more verbose code

## Alternatives Considered

### 1. Lazy Initialization
```kotlin
private val _factory by lazy {
    runBlocking { encryptedRoomFactory.getFactoryFor("db") }
}
```
**Rejected**: Lazy initialization happens on first access, which could be unpredictable timing.

### 2. CoroutineScope Injection
```kotlin
class EncryptedRoomFactory(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    fun getFactoryFor(dbName: String): CompletableFuture<SupportFactory> {
        return scope.async { /* ... */ }.asCompletableFuture()
    }
}
```
**Rejected**: Overly complex for a simple use case. Adds unnecessary abstraction.

### 3. Keep runBlocking Inside
```kotlin
fun getFactoryFor(dbName: String): SupportFactory {
    return runBlocking { /* ... */ }
}
```
**Rejected**: This was the original problematic pattern. Doesn't solve the thread safety issue.

## References
- [Kotlin Coroutines Best Practices](https://kotlinlang.org/docs/coroutines-basics.html)
- [Android Coroutines Guide](https://developer.android.com/kotlin/coroutines)
- ADR-002: MVI Pattern for UI State Management

---

# ADR-002: ScratchModule Security Enhancement

**Status:** Accepted  
**Date:** 2024-09-24  
**Authors:** Security Team  
**Context:** Hardcoded SQLCipher passphrase in ScratchModule

---

## Context

The `ScratchModule` was using a hardcoded string passphrase for SQLCipher encryption:

```kotlin
// INSECURE PATTERN
val passphrase = SQLiteDatabase.getBytes("alloy_scratch_passphrase_keystore_secured".toCharArray())
val factory = SupportFactory(passphrase)
```

This provides minimal security because:
1. The passphrase is derived from a static string
2. No secure key generation
3. No Keystore encryption
4. Easily reverse-engineered from APK

Meanwhile, `ClipModule` uses the proper `EncryptedRoomFactory` with secure key management.

## Decision

We will standardize on `EncryptedRoomFactory` for ALL encrypted databases:

```kotlin
// SECURE PATTERN
@Provides
@Singleton
fun provideScratchDatabase(
    @ApplicationContext context: Context,
    encryptedRoomFactory: EncryptedRoomFactory
): ScratchDatabase {
    val factory = runBlocking(Dispatchers.IO) {
        encryptedRoomFactory.getFactoryFor("alloy_scratch")
    }
    
    return Room.databaseBuilder(...)
        .openHelperFactory(factory)
        .build()
}
```

This ensures:
- ✅ Secure random key generation (32 bytes)
- ✅ Keystore encryption for key protection
- ✅ Secure storage in DataStore
- ✅ Consistent security across all databases

## Consequences

### Positive
- ✅ Uniform security model across all encrypted databases
- ✅ Keys protected by Android Keystore hardware
- ✅ No hardcoded secrets in code
- ✅ Easier key rotation and management

### Negative
- ⚠️ Scratch data will be inaccessible after upgrade (requires migration strategy)
- ⚠️ Slightly larger APK due to shared dependencies

## Migration Plan

For existing users with scratch data:
1. Detect old unencrypted database on first launch
2. Export data to secure location
3. Create new encrypted database
4. Import data with encryption
5. Delete old unencrypted database

## References
- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-android/)
- Security Audit Report 2024-09-24

---

# ADR-003: Module Structure and Dependency Graph

**Status:** Accepted  
**Date:** 2024-09-24  
**Authors:** Architecture Team  
**Context:** Modular Android application structure

---

## Context

The Alloy project follows a modular architecture with the following module types:
- `:app` - Application shell and navigation
- `:core:*` - Shared libraries (common, design, datastore, proc, netlocal)
- `:modules:*` - Feature modules (statspill, scenes, clip, scratch)
- `:tooling:*` - Utility applications
- `:lint` - Custom lint rules

## Decision

We will maintain the current hierarchical module structure:

```
:app
├── :core:common (base)
├── :core:design (UI)
├── :core:datastore (persistence)
├── :core:proc (system telemetry)
├── :core:netlocal (network)
├── :modules:statspill
├── :modules:scenes
├── :modules:clip
├── :modules:scratch
├── :tooling:probe
└── :lint
```

**Key Principles:**
1. **No circular dependencies** - Strict DAG structure
2. **Feature modules depend only on core** - No inter-feature dependencies
3. **Core modules have clear boundaries** - Single responsibility
4. **App module is the root** - Depends on all modules but is not depended upon

## Consequences

### Positive
- ✅ Independent module compilation
- ✅ Faster build times for feature development
- ✅ Clear separation of concerns
- ✅ Easy to test modules in isolation
- ✅ Supports Dynamic Feature Modules in future

### Negative
- ⚠️ More complex Gradle configuration
- ⚠️ Requires careful dependency management
- ⚠️ Some code duplication across modules (shared UI components)

## Future Considerations

When adding new modules:
1. **New Core Module**: Create when functionality is shared across 2+ feature modules
2. **New Feature Module**: Follow standard pattern (ViewModel, Screen, DAO, Database)
3. **Dynamic Feature Module**: Implement DFM infrastructure first (ADR-004)

## References
- [Android Modularization Guide](https://developer.android.com/build/configure-app-module)
- ADR-001: Database Factory Initialization Pattern
- Macro-Architecture Review 2024-09-24

---

# ADR-004: Build Warning Policy

**Status:** Accepted  
**Date:** 2024-09-24  
**Authors:** Quality Team  
**Context:** Code quality enforcement

---

## Context

Build warnings often indicate:
- Potential bugs
- Code quality issues
- Technical debt
- Future maintenance problems

Ignoring warnings leads to code quality degradation over time.

## Decision

**Build warnings are NOT ALLOWED unless well-justified.**

### Enforcement Mechanisms

1. **Gradle Lint**: `warningsAsErrors = true`
2. **Detekt**: `failFast = true`
3. **Pre-commit Hook**: Checks for `@Suppress` without justification
4. **Code Review**: Requires explanation for all suppressions

### Allowed Exceptions

Warnings may be suppressed ONLY when:

1. **False Positive**: Demonstrably incorrect warning
   ```kotlin
   @Suppress("UnusedPrivateMember") // Used by reflection
   ```

2. **Legacy Compatibility**: Fixing would break compatibility
   - Must have migration plan documented
   - Must be tracked as GitHub issue

3. **Third-party Limitation**: Warning from external dependency
   - Must verify it's not configuration issue
   - Must track for update when library fixes it

### Required Justification Format

```kotlin
@Suppress("WarningName") // Brief justification + reference to issue/plan
val timeoutSeconds = 30  // Config value, see issue #123
```

## Consequences

### Positive
- ✅ Higher code quality
- ✅ Catch bugs earlier
- ✅ Reduced technical debt
- ✅ Clear documentation of exceptions

### Negative
- ⚠️ Slower development initially
- ⚠️ Requires discipline and review
- ⚠️ May need refactoring of existing code

## References
- ANDROID_BEST_PRACTICES.md Section 10
- Code Quality Summary 2024-09-24

---

# ADR-005: Generated Documentation Storage Policy

**Status:** Accepted  
**Date:** 2024-09-24  
**Authors:** Documentation Team  
**Context:** Managing generated markdown files

---

## Context

During development, various markdown files are generated:
- Audit reports
- Analysis results
- Work plans
- Quality summaries
- Temporary documentation

These files clutter the root directory and make it hard to distinguish permanent documentation from temporary artifacts.

## Decision

All non-documentation `.md` files must be stored in `.md-storage/` with lifecycle management:

```
.md-storage/
├── audit-reports/    # 6 months lifetime
├── analysis/         # 3 months lifetime
├── planning/         # Project end lifetime
├── summaries/        # 3 months lifetime
├── temporary/        # 2 weeks lifetime
└── archived/         # Archived files
```

### File Naming Convention

```
YYYY-MM-DD-description.md
```

Examples:
- `2024-09-24-antipatterns-audit.md`
- `2024-09-24-work-plan.md`
- `2024-09-24-code-quality-summary.md`

### Lifecycle Rules

| Stage | Age | Action |
|-------|-----|--------|
| Fresh | 0-30 days | Active use, may be referenced |
| Review | 30-90 days | Verify still relevant |
| Stale | 90+ days | Archive |
| Expired | 180+ days | Delete |

### Enforcement

1. **Pre-commit Hook**: Warns about generated MD files in root
2. **Cleanup Script**: `./scripts/cleanup-md-storage.sh`
3. **Move Script**: `./scripts/move-to-storage.sh`

## Consequences

### Positive
- ✅ Clean root directory
- ✅ Clear distinction between permanent and temporary docs
- ✅ Automated lifecycle management
- ✅ Easier onboarding for new developers

### Negative
- ⚠️ Requires discipline to use correctly
- ⚠️ Additional scripts to maintain
- ⚠️ May need to update symlinks

## References
- ANDROID_BEST_PRACTICES.md Section 11
- Scripts: cleanup-md-storage.sh, move-to-storage.sh

---

# ADR-006: Third-Party License Policy

**Status:** Accepted  
**Date:** 2024-09-24  
**Authors:** Legal/Security Team  
**Context:** Third-party dependency license compliance

---

## Context

The project uses numerous third-party libraries. Each library has a license that governs usage rights and obligations. Using libraries with incompatible licenses can create legal liability.

## Decision

**ALL third-party dependencies MUST have fully permissive licenses.**

### Approved Licenses

| License | Status | Notes |
|---------|--------|-------|
| MIT | ✅ Approved | Fully permissive |
| Apache 2.0 | ✅ Approved | Fully permissive, patent grant |
| BSD 2-Clause | ✅ Approved | Fully permissive |
| BSD 3-Clause | ✅ Approved | Fully permissive |
| ISC | ✅ Approved | Fully permissive |
| CC0-1.0 | ✅ Approved | Public domain |
| Unlicense | ✅ Approved | Public domain |

### Prohibited Licenses

| License | Status | Reason |
|---------|--------|--------|
| GPL 2.0/3.0 | ❌ Prohibited | Copyleft |
| AGPL 3.0 | ❌ Prohibited | Strong copyleft |
| SSPL 1.0 | ❌ Prohibited | Field-of-use restrictions |
| Proprietary | ❌ Prohibited | Commercial restrictions |

### Conditional Licenses (Require Review)

| License | Status | Requirements |
|---------|--------|--------------|
| LGPL 2.1/3.0 | ⚠️ Conditional | Dynamic linking only |
| MPL 2.0 | ⚠️ Conditional | Source disclosure |
| EPL 2.0 | ⚠️ Conditional | Legal review required |

### Enforcement

1. **License Checker**: `./scripts/check-licenses.sh`
2. **Pre-commit Hook**: Warns about new dependencies
3. **Documentation**: `third-party-licenses.md` report
4. **Quarterly Audit**: Review all dependencies

## Consequences

### Positive
- ✅ Legal compliance
- ✅ No copyleft obligations
- ✅ Clear audit trail
- ✅ Automated verification

### Negative
- ⚠️ May limit library choices
- ⚠️ Requires manual review for conditional licenses
- ⚠️ Additional maintenance overhead

## References
- ANDROID_BEST_PRACTICES.md Section 12
- SPDX License List
- third-party-licenses.md
