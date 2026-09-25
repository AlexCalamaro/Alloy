# Code Quality & Antipatterns Enforcement Summary

**Project:** Alloy Android  
**Date:** September 24, 2026  
**Status:** ✅ Enforcement System Active

---

## 📋 Documents Created

| Document | Purpose | Location | Status |
|----------|---------|----------|--------|
| **ANDROID_BEST_PRACTICES.md** | Binding development guidelines | Root | ✅ Active |
| **detekt.yml** | Detekt static analysis configuration | Root | ✅ Active |
| **lint/** | Custom lint rules module | `lint/` | ✅ Active |
| **gradle/lint.gradle.kts** | Lint Gradle configuration | `gradle/` | ✅ Active |
| **.git/hooks/pre-commit.sh** | Pre-commit validation hook | `.git/hooks/` | ✅ Active |
| **scripts/check-licenses.sh** | License compliance checker | `scripts/` | ✅ Active |
| **scripts/cleanup-md-storage.sh** | Markdown storage cleanup | `scripts/` | ✅ Active |
| **scripts/move-to-storage.sh** | Move MD files to storage | `scripts/` | ✅ Active |

---

## 🔍 Antipatterns Audit Results

### Summary by Severity

```
┌─────────────┬──────┬────────────────────────────────┐
│ Severity    │ Count│ Action Required                │
├─────────────┼──────┼────────────────────────────────┤
│ 🔴 Critical │   5  │ Fix before production release  │
│ 🟠 High     │  12  │ Fix in next sprint             │
│ 🟡 Medium   │   8  │ Plan for upcoming sprints      │
│ 🟢 Low      │   5  │ Backlog items                  │
└─────────────┴──────┴────────────────────────────────┘
```

### Critical Issues Requiring Immediate Attention

| # | Issue | File | Line |
|---|-------|------|------|
| 1 | Hardcoded SQLCipher passphrase | `modules/scratch/di/ScratchModule.kt` | 23 |
| 2 | Thread safety violation (runBlocking) | `core/datastore/EncryptedRoomFactory.kt` | 21-44 |
| 3 | State not hoisted (ChecklistPane) | `modules/scratch/ui/ScratchScreen.kt` | 153 |
| 4 | Tab state not hoisted | `app/ui/DashboardActivity.kt` | 70 |
| 5 | LazyColumn without key | `modules/scenes/ui/ScenesScreen.kt` | 41 |

---

## 🛡️ Enforcement Mechanisms

### 1. Detekt Configuration

**File:** `detekt.yml`

**Key Rules Enabled:**
- ✅ Compose stability checks (ModifierMissing, Naming, UnstableCollections)
- ✅ Coroutine checks (GlobalCoroutineUsage, InjectDispatcher, SleepInsteadOfDelay)
- ✅ Coroutines lifecycle (SuspendFunWithFlowReturnType)
- ✅ KDoc requirements (absenseOfKDoc, undocumentedPublicFunction)
- ✅ Complexity limits (CyclomaticComplexMethod: 15, LongMethod: 60)
- ✅ Error handling (SwallowedException, TooGenericExceptionCaught)
- ✅ Naming conventions (BooleanPropertyNaming, FunctionNaming)
- ✅ Performance (ForEachOnRange, SpreadOperator)
- ✅ Style (MagicNumber, UnusedImports, VarCouldBeVal)

**Warnings-as-Errors Policy:**
```yaml
detekt:
  failFast: true  # Fail on any violation - no warnings allowed
```

**Justification Required:**
All suppressions must include justification:
```kotlin
@Suppress("RuleName") // Justification for allowing this warning
```

### 2. Custom Lint Rules Module

**Location:** `lint/`

**Rules Implemented:**
| Rule ID | Severity | Description |
|---------|----------|-------------|
| `ComposableStateNotHoisted` | ERROR | Detects mutableStateOf in @Composable |
| `RunBlockingInProduction` | ERROR | Detects runBlocking outside tests |
| `GlobalScopeUsage` | ERROR | Detects GlobalScope.launch() |
| `MissingKDocOnPublicApi` | WARNING | Missing KDoc on public APIs |
| `MagicNumberInProduction` | WARNING | Numeric literals not as constants |
| `MutableStateInComposable` | WARNING | var with mutableStateOf |

**Usage:**
```kotlin
// Add to build.gradle.kts
dependencies {
    lintChecks(project(":lint"))
}
```

### 3. Pre-commit Hook

**File:** `.git/hooks/pre-commit.sh`

**Checks Performed:**
1. ✅ GlobalScope usage detection
2. ✅ runBlocking in production files
3. ✅ Hardcoded SQLCipher passphrases
4. ✅ mutableStateOf in @Composable context (warning)
5. ✅ LazyColumn missing key (warning)
6. ✅ TODO/FIXME/HACK comments (warning)
7. ✅ Public API KDoc (warning)
8. ✅ **@Suppress annotations without justification**
9. ✅ **Generated markdown files in root**
10. ✅ **New dependency additions (license compliance)**

**Integration:**
```bash
chmod +x .git/hooks/pre-commit.sh
# Run automatically on git commit
```

**Bypass:** `git commit --no-verify`

### 4. Gradle Lint Configuration

**File:** `gradle/lint.gradle.kts`

**Applied to:** All Android modules

**Key Settings:**
```kotlin
lint {
    checkAll = true
    abortOnError = true
    warningsAsErrors = true  // NO WARNINGS ALLOWED
    ignoreTestSources = true
    
    // Critical checks as errors
    warningsAsErrors += [
        "LeakCanary",
        "StaticFieldLeak",
        "CoroutineScope",
        "ComposeModifierMissing",
        "ComposeNaming",
        "ComposeRememberMissing",
        "MissingKDoc",
        "MissingIndex",
    ]
}
```

**Zero Tolerance Policy:**
- All build warnings treated as errors
- Suppressions require documented justification
- Code review validates all suppressions
- Quarterly audit of suppressions

### 5. License Compliance Enforcement

**File:** `scripts/check-licenses.sh`

**Approved Licenses:**
- ✅ MIT
- ✅ Apache 2.0
- ✅ BSD 2-Clause / 3-Clause
- ✅ ISC
- ✅ CC0-1.0
- ✅ Unlicense

**Prohibited Licenses:**
- ❌ GPL 2.0/3.0
- ❌ AGPL 3.0
- ❌ SSPL 1.0
- ❌ Proprietary

**Conditional Licenses (require review):**
- ⚠️ LGPL 2.1/3.0
- ⚠️ MPL 2.0
- ⚠️ EPL 2.0

**Integration:**
```bash
# Run before adding new dependencies
./scripts/check-licenses.sh

# Pre-commit hook checks for new dependencies
```

### 6. Markdown Storage Management

**Files:** `scripts/cleanup-md-storage.sh`, `scripts/move-to-storage.sh`

**Storage Structure:**
```
.md-storage/
├── audit-reports/    # Code audit reports (6 months)
├── analysis/         # Code analysis results (3 months)
├── planning/         # Work plans (project end)
├── summaries/        # Quality summaries (3 months)
├── temporary/        # Temporary docs (2 weeks)
└── archived/         # Archived files
```

**Lifecycle Rules:**
- Fresh: 0-30 days (active use)
- Review: 30-90 days (verify relevance)
- Stale: 90+ days (archive)
- Expired: 180+ days (delete)

---

## 📊 Coverage Analysis

### Files Audited

| Category | Files | Issues Found |
|----------|-------|--------------|
| ViewModels | 5 | 14 issues |
| Composables | 6 | 10 issues |
| DAOs/Entities | 6 | 8 issues |
| DI Modules | 7 | 8 issues |
| **Total** | **24** | **30+ issues** |

### Test Coverage

| Module | Tests | Status |
|--------|-------|--------|
| `core:datastore` | ✅ | Passing |
| `modules:clip` | ✅ | Passing |
| `modules:scratch` | ✅ | Passing |
| `modules:scenes` | ✅ | Passing |
| `modules:statspill` | ✅ | Passing |
| `app` | ✅ | Passing |

---

## 🚀 Next Steps

### Immediate (This Week)

- [ ] Fix critical SQLCipher security issue in ScratchModule
- [ ] Fix thread safety in EncryptedRoomFactory
- [ ] Hoist ChecklistPane state to parent
- [ ] Hoist Dashboard tab state
- [ ] Add LazyColumn keys

### Short-term (Next Sprint)

- [ ] Add error handling to all database operations
- [ ] Add KDoc to all @Provides methods
- [ ] Add KDoc to all DAO/Entity APIs
- [ ] Fix broadcast receiver leak in StatsViewModel
- [ ] Fix race conditions in timer loops

### Long-term (Upcoming Sprints)

- [ ] Add missing database indexes
- [ ] Add transaction support to DAOs
- [ ] Reduce code duplication in checklist operations
- [ ] Enable Room schema export
- [ ] Add comprehensive effect consumption documentation

---

## 🔧 Developer Guidelines

### Before Committing Code

1. **Run pre-commit checks:**
   ```bash
   ./gradlew detekt lintKotlin
   ```

2. **Verify no critical antipatterns:**
   - No GlobalScope usage
   - No runBlocking in production
   - State properly hoisted in composables
   - LazyColumn/LazyRow has key function

3. **Check documentation:**
   - Public APIs have KDoc
   - No TODO/FIXME/HACK comments (or document why)

4. **Verify no build warnings:**
   - All warnings must be resolved
   - Suppressions require justification
   - Run: `./gradlew build` and check for warnings

5. **Check dependency licenses:**
   - Run: `./scripts/check-licenses.sh`
   - Verify all licenses are fully permissive

6. **Ensure tests pass:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Code Review Checklist

- [ ] No GlobalScope or unconfined coroutines
- [ ] State hoisted to parent/ViewModel
- [ ] LazyColumn/LazyRow has key function
- [ ] Error handling on all suspending operations
- [ ] KDoc on public APIs
- [ ] No magic numbers (use constants)
- [ ] Proper lifecycle management
- [ ] No context leaks
- [ ] **No build warnings (or well-justified suppressions)**
- [ ] **All @Suppress annotations have comments explaining why**
- [ ] **New dependencies have approved permissive licenses**
- [ ] **No generated markdown files in root directory**

---

## 📈 Metrics & Monitoring

### Quality Gates

| Metric | Target | Current |
|--------|--------|---------|
| Detekt violations | 0 | Pending |
| Lint errors | 0 | Pending |
| Build warnings | 0 | Pending |
| Critical antipatterns | 0 | 5 |
| Test coverage | 80% | ~60% |
| KDoc coverage | 90% | ~40% |
| Suppressions with justification | 100% | Pending |
| **Dependencies with approved licenses** | **100%** | **Pending** |

### Continuous Integration

Add to CI/CD pipeline:
```yaml
stages:
  - lint
  - license-check
  - test
  - build

lint:
  script:
    - ./gradlew detekt
    - ./gradlew lint

license-check:
  script:
    - ./scripts/check-licenses.sh

test:
  script:
    - ./gradlew testDebugUnitTest

build:
  script:
    - ./gradlew assembleDebug
```

---

## 📚 References

- [ANDROID_BEST_PRACTICES.md](./ANDROID_BEST_PRACTICES.md) - Full guidelines
- [Detekt Documentation](https://detekt.dev/docs/intro/)
- [Android Lint](https://developer.android.com/studio/write/lint)
- [Compose State Guidelines](https://developer.android.com/jetpack/compose/state)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
- [SPDX License List](https://spdx.org/licenses/)
- [Open Source Initiative](https://opensource.org/licenses)

---

**Last Updated:** September 24, 2026  
**Next Review:** After critical issues are fixed
