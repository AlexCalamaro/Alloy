# Android Antipatterns Audit Report

**Project:** Alloy  
**Audit Date:** September 24, 2026  
**Scope:** Full codebase review against ANDROID_BEST_PRACTICES.md  
**Status:** ⚠️ 30+ issues identified

---

## Executive Summary

| Severity | Count | Status |
|----------|-------|--------|
| 🔴 Critical | 5 | Must fix before production |
| 🟠 High | 12 | Should fix in next sprint |
| 🟡 Medium | 8 | Plan for upcoming sprints |
| 🟢 Low | 5 | Backlog items |

---

## 🔴 Critical Issues (Must Fix)

### 1. Hardcoded SQLCipher Passphrase in ScratchModule
**File:** `modules/scratch/src/main/java/.../di/ScratchModule.kt:23`  
**Severity:** Critical (Security)

**Issue:** Uses hardcoded string passphrase instead of secure key management:
```kotlin
val passphrase = SQLiteDatabase.getBytes("alloy_scratch_passphrase_keystore_secured".toCharArray())
```

**Impact:** Scratchpad data is significantly less protected than clipboard data.

**Fix Required:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ScratchModule {
    @Provides
    @Singleton
    fun provideScratchDatabase(
        @ApplicationContext context: Context,
        encryptedRoomFactory: EncryptedRoomFactory
    ): ScratchDatabase {
        val factory = encryptedRoomFactory.getFactoryFor("alloy_scratch")
        return Room.databaseBuilder(context, ScratchDatabase::class.java, "alloy_scratch.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(false)
            .build()
    }
}
```

---

### 2. Thread Safety Violation in EncryptedRoomFactory
**File:** `core/datastore/src/main/java/.../EncryptedRoomFactory.kt:21-44`  
**Severity:** Critical (Stability)

**Issue:** `runBlocking` called inside synchronous method with suspend functions:
```kotlin
fun getFactoryFor(dbName: String): SupportFactory {
    val passphraseBytes = runBlocking {  // ❌ Blocks thread
        dataStoreManager.setString("${dbName}_iv", ...)  // ❌ Suspend in runBlocking
        // ...
    }
}
```

**Impact:** Potential ANR on main thread; incorrect suspend function behavior.

**Fix Required:**
```kotlin
suspend fun getFactoryFor(dbName: String): SupportFactory {
    val passphraseBytes = withContext(Dispatchers.IO) {
        // ... async operations
    }
    return SupportFactory(passphraseBytes)
}
```

---

### 3. State Created in Sub-Composable (Not Hoisted)
**File:** `modules/scratch/src/main/java/.../ui/ScratchScreen.kt:153`  
**Severity:** Critical (UX)

**Issue:** `mutableStateOf` created inside `ChecklistPane` sub-composable:
```kotlin
@Composable
fun ChecklistPane(...) {
    var newItemText by remember { mutableStateOf("") }  // ❌ Lost on recomposition
    // ...
}
```

**Impact:** State resets on any parent recomposition, causing data loss.

**Fix Required:** Hoist state to parent `ScratchScreen` and pass as parameters.

---

### 4. Tab State Not Hoisted in Dashboard
**File:** `app/src/main/java/.../ui/DashboardActivity.kt:70`  
**Severity:** Critical (UX)

**Issue:** `activeTab` state created inside `DashboardScreen`:
```kotlin
@Composable
fun DashboardScreen(...) {
    var activeTab by remember { mutableStateOf(ModuleTab.STATS_PILL) }  // ❌
    // ...
}
```

**Impact:** Tab selection resets on any recomposition.

**Fix Required:** Move state management to ViewModel or parent composable.

---

### 5. LazyColumn Without Key Function
**File:** `modules/scenes/src/main/java/.../ui/ScenesScreen.kt:41`  
**Severity:** Critical (Performance)

**Issue:** `items(uiState.scenes)` without key function:
```kotlin
LazyColumn {
    items(uiState.scenes) { scene ->  // ❌ No key
        // ...
    }
}
```

**Impact:** Incorrect item recycling causes UI glitches and performance issues.

**Fix Required:**
```kotlin
LazyColumn {
    items(uiState.scenes, key = { it.id }) { scene ->
        // ...
    }
}
```

---

## 🟠 High Severity Issues

### 6. Missing Error Handling in Database Creation
**Files:** `ClipModule.kt:27-30`, `ScratchModule.kt:26-29`  
**Severity:** High (Reliability)

**Issue:** No try-catch around `Room.databaseBuilder().build()`:
```kotlin
return Room.databaseBuilder(context, ClipDatabase::class.java, "alloy_clip.db")
    .openHelperFactory(factory)
    .build()  // ❌ Can throw SQLException
```

**Fix Required:** Add try-catch with proper error logging and fallback.

---

### 7. Missing KDoc on @Provides Methods
**Files:** `ClipModule.kt`, `ScratchModule.kt`, `DispatchersModule.kt`, `AppModule.kt`  
**Severity:** High (Maintainability)

**Issue:** No documentation on 12+ public `@Provides` methods.

**Fix Required:** Add KDoc to all `@Provides` methods explaining purpose, parameters, and return values.

---

### 8. Missing KDoc on DAO Methods
**Files:** `ClipDao.kt`, `ScratchDao.kt`  
**Severity:** High (Maintainability)

**Issue:** No KDoc on any DAO methods.

**Fix Required:** Add KDoc to all public DAO methods.

---

### 9. Missing KDoc on Entity Classes
**Files:** `ClipEntity.kt`, `ScratchEntity.kt`  
**Severity:** High (Maintainability)

**Issue:** No KDoc on entities or properties.

**Fix Required:** Add KDoc explaining each field's purpose.

---

### 10. State Accessed in Coroutine Loop (Race Condition)
**File:** `modules/scratch/src/main/java/.../ScratchViewModel.kt:218-220, 234-236`  
**Severity:** High (Stability)

**Issue:** `uiState.value.isTimerRunning` checked in while loop:
```kotlin
while (uiState.value.isTimerRunning) {  // ❌ Stale state check
    delay(1000L)
    updateState { it.copy(timerSeconds = it.timerSeconds + 1) }
}
```

**Fix Required:** Use `AtomicBoolean` or separate StateFlow for running status.

---

### 11. Broadcast Receiver Leak Risk
**File:** `modules/statspill/src/main/java/.../StatsViewModel.kt:129-134`  
**Severity:** High (Memory Leak)

**Issue:** Receiver assigned even if registration fails:
```kotlin
try {
    context.registerReceiver(batteryReceiver, filter)
} catch (e: Exception) {
    // ❌ receiver still assigned
}
```

**Fix Required:** Only assign receiver if registration succeeds.

---

### 12. stopTimer() Should Be Private
**File:** `modules/scratch/src/main/java/.../ScratchViewModel.kt:225`  
**Severity:** High (MVI Pattern Violation)

**Issue:** `stopTimer()` is public but should only be called internally.

**Fix Required:** Make `stopTimer()` private.

---

### 13. Missing Error Handling in Database Collection
**File:** `modules/clip/src/main/java/.../ClipViewModel.kt:95-107`  
**Severity:** High (Reliability)

**Issue:** No `.catch {}` on database collection:
```kotlin
viewModelScope.launch {
    clipDao.getAllClips().collect { items ->  // ❌ No error handling
        // ...
    }
}
```

**Fix Required:** Add `.catch { e -> /* handle error */ }` to flow.

---

### 14. Missing Error Handling in Stats Polling
**File:** `modules/statspill/src/main/java/.../StatsViewModel.kt:171-180`  
**Severity:** High (Reliability)

**Issue:** `pollVitals()` crashes silently on exception.

**Fix Required:** Add try-catch around `procReader.readMemInfo()` and `readCpuUsagePercent()`.

---

## 🟡 Medium Severity Issues

### 15-18. Magic Numbers Without Constants
**Files:** `ClipViewModel.kt:217`, `ScratchViewModel.kt:218,234`, `StatsViewModel.kt:161`

**Issues:**
- `3600000` (cleanup interval)
- `1000L` (timer/polling intervals)

**Fix Required:** Define named constants:
```kotlin
private const val CLEANUP_INTERVAL_MS = 3600_000L
private const val TIMER_UPDATE_INTERVAL_MS = 1000L
private const val POLLING_INTERVAL_MS = 1000L
```

---

### 19. LazyColumn Missing Key in StopwatchPane
**File:** `modules/scratch/src/main/java/.../ui/ScratchScreen.kt:298`  
**Severity:** Medium (Performance)

**Fix Required:** Add `key = { index }` to laps list.

---

### 20. Inefficient List Reversal in StopwatchPane
**File:** `modules/scratch/src/main/java/.../ui/ScratchScreen.kt:300`  
**Severity:** Medium (Performance)

**Issue:** `laps.reversed().withIndex().toList()` creates new list every composition.

**Fix Required:** Use `remember` or pre-compute in ViewModel.

---

### 21. Missing Index on isPinned Column
**File:** `modules/clip/src/main/java/.../ClipEntity.kt`  
**Severity:** Medium (Performance)

**Issue:** Query orders by `isPinned DESC` without index.

**Fix Required:** Add `@Index(columns = ["isPinned"])` to entity.

---

### 22. Missing Index on updatedAt Column
**File:** `modules/scratch/src/main/java/.../ScratchEntity.kt`  
**Severity:** Medium (Performance)

**Fix Required:** Add `@Index(columns = ["updatedAt"])` to entity.

---

### 23. Missing Transaction Support in DAOs
**Files:** `ClipDao.kt`, `ScratchDao.kt`  
**Severity:** Medium (Data Integrity)

**Fix Required:** Add `@Transaction` methods for batch operations.

---

### 24. stopTimer() Public in ScratchViewModel
**File:** `modules/scratch/src/main/java/.../ScratchViewModel.kt:225`  
**Severity:** Medium (MVI Pattern)

**Fix Required:** Make private.

---

### 25. Missing Effect Consumption Documentation
**File:** `core/common/src/main/java/.../BaseViewModel.kt:37-38`  
**Severity:** Medium (Documentation)

**Fix Required:** Add KDoc warning about effect consumption requirements.

---

### 26. Hardcoded Test Data in Production ViewModel
**File:** `modules/clip/src/main/java/.../ClipViewModel.kt:87-92`  
**Severity:** Medium (Code Quality)

**Fix Required:** Remove hardcoded clips from constructor.

---

### 27. Missing KDoc on Public Methods
**Files:** `ClipViewModel.kt`, `ScratchViewModel.kt`, `StatsViewModel.kt`  
**Severity:** Medium (Documentation)

**Fix Required:** Add KDoc to all public methods.

---

### 28. No State Initialization from Repository
**File:** `modules/scenes/src/main/java/.../ScenesViewModel.kt:38-55`  
**Severity:** Medium (Architecture)

**Fix Required:** Consider adding SceneRepository for dynamic state.

---

## 🟢 Low Severity Issues

### 29. Missing KDoc on Helper Functions
**Files:** `ClipScreen.kt`, `ScratchScreen.kt`  
**Severity:** Low (Documentation)

**Fix Required:** Add KDoc to helper functions like `formatTimestamp()`.

---

### 30. Code Duplication in Checklist Operations
**File:** `modules/scratch/src/main/java/.../ScratchViewModel.kt:251-288`  
**Severity:** Low (Maintainability)

**Fix Required:** Consider helper class for checklist operations.

---

### 31. Missing KDoc on Modifier Parameters
**Files:** `ClipScreen.kt:42`, `ScenesScreen.kt:30`  
**Severity:** Low (Documentation)

**Fix Required:** Add `@param modifier` to KDoc.

---

### 32. exportSchema = false in Room Databases
**Files:** `ClipDatabase.kt:6`, `ScratchDatabase.kt:6`  
**Severity:** Low (Maintainability)

**Fix Required:** Set `exportSchema = true` for migration support.

---

### 33. Unused Import in ScratchModule
**File:** `modules/scratch/src/main/java/.../di/ScratchModule.kt:12-13`  
**Severity:** Low (Code Quality)

**Fix Required:** Verify or remove redundant imports.

---

## Enforcement Mechanisms

### Immediate Actions

1. **Add Lint Rules** - Create custom lint rules for:
   - Missing LazyColumn keys
   - State created in composables
   - Missing KDoc on public APIs
   - Magic numbers without constants
   - **Build warnings without justification**

2. **Pre-commit Hooks** - Add checks for:
   - Critical antipatterns
   - Test coverage requirements
   - KDoc completeness
   - **@Suppress annotations without justification**

3. **CI/CD Integration** - Add to pipeline:
   - Detekt configuration
   - Lint checks
   - Test coverage thresholds
   - **Warnings-as-errors policy**

### Long-term Actions

1. **Refactoring Sprint** - Dedicate sprint to fixing all high/medium issues
2. **Code Review Checklist** - Update review process to check for antipatterns
3. **Developer Training** - Document patterns and anti-patterns for team

---

## Priority Fix Order

| Priority | Issue | Estimated Effort |
|----------|-------|------------------|
| 1 | Fix ScratchModule encryption | 2 hours |
| 2 | Fix EncryptedRoomFactory threading | 1 hour |
| 3 | Hoist ChecklistPane state | 2 hours |
| 4 | Hoist Dashboard tab state | 1 hour |
| 5 | Add LazyColumn keys | 30 min |
| 6 | Add error handling to databases | 2 hours |
| 7 | Add KDoc to all @Provides | 1 hour |
| 8 | Add KDoc to DAOs/Entities | 1 hour |
| 9 | Fix race conditions in timers | 2 hours |
| 10 | Fix broadcast receiver leak | 1 hour |

**Total Estimated Effort:** ~14 hours

---

## Next Steps

1. ✅ Audit complete - all issues documented
2. ⏳ Create enforcement lint rules
3. ⏳ Fix critical issues (1-5)
4. ⏳ Fix high severity issues (6-14)
5. ⏳ Schedule medium severity fixes
6. ⏳ Backlog low severity items
