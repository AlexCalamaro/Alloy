# Macro-Architecture Review Report

**Project:** Alloy Android  
**Review Date:** September 24, 2026  
**Reviewer:** Architecture Team  
**Scope:** High-level project structure, modularity, extensibility

---

## Executive Summary

| Metric | Score | Status |
|--------|-------|--------|
| **Overall Architecture** | 7/10 | ✅ Good |
| **Build Structure** | B+ (85/100) | ✅ Good |
| **Extensibility** | 7/10 | ⚠️ Moderate |
| **Scalability** | 8/10 | ✅ Good |

The Alloy project demonstrates a strong architectural foundation with clear adherence to modern Android development practices. The modular structure, comprehensive lint rules, and well-documented patterns position it well for scaling.

---

## 1. Module Structure

### 1.1 Current Module Inventory

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              :app                                            │
│                    Main Application Shell                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┬─────────────┴──────┐
        │                           │                           │                    │
        ▼                           ▼                           ▼                    ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐    ┌───────────────┐
│ core:common   │         │ core:design   │         │ core:datastore│    │ core:proc     │
│ ───────────── │         │ ───────────── │         │ ───────────── │    │ ───────────── │
│ - DI          │         │ - Theme       │         │ - Room/SQL    │    │ - /proc       │
│ - Events      │         │ - Desktop UX  │         │ - DataStore   │    │ - CPU/RAM     │
│ - BaseVM      │         │               │         │               │    │               │
└───────────────┘         └───────────────┘         └───────────────┘    └───────────────┘
        │                           │                           │                    │
        │                           │                           │                    │
        ▼                           ▼                           ▼                    │
┌───────────────┐         ┌───────────────┐         ┌───────────────┐    ┌───────────────┐
│ modules:      │         │ modules:      │         │ modules:      │    │ modules:      │
│  statspill    │         │  scenes       │         │  clip         │    │  scratch      │
│ ───────────── │         │ ───────────── │         │ ───────────── │    │ ───────────── │
│ - Widget      │         │ - Launcher    │         │ - Clipboard   │    │ - Scratchpad  │
│ - Overlay     │         │ - Windows     │         │ - Transform   │    │ - Multi-inst  │
└───────────────┘         └───────────────┘         └───────────────┘    └───────────────┘

Additional:
- core:netlocal (Ktor loopback)
- tooling:probe (platform validation)
- lint (custom lint rules)
```

### 1.2 Dependency Analysis

**Status: ✅ NO CIRCULAR DEPENDENCIES**

The dependency structure is a strict Directed Acyclic Graph (DAG):
- `:app` depends on all modules but is not depended upon
- Core modules form the foundation layer
- Feature modules depend only on core modules
- No module-to-module dependencies (excellent separation)

### 1.3 Module Granularity Assessment

| Module Type | Count | Assessment |
|-------------|-------|------------|
| Core Libraries | 5 | ✅ Appropriate |
| Feature Modules | 4 | ✅ Appropriate |
| Tooling Apps | 1 | ✅ Appropriate |
| Lint Module | 1 | ✅ Appropriate |
| **Total** | **12** | ✅ Good for current scale |

---

## 2. Build Configuration

### 2.1 Gradle Optimization

**Configuration:**
```kotlin
// gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

**Assessment: ✅ Excellent** - All recommended optimizations enabled

### 2.2 Version Catalog

| Category | Version | Status |
|----------|---------|--------|
| AGP | 9.4.1 | ✅ Latest |
| Kotlin | 2.0.21 | ✅ Modern |
| KSP | 2.0.21-1.0.28 | ✅ Consistent |
| Compose BOM | 2025.02.00 | ✅ Recent |
| Hilt | 2.60.1 | ✅ Stable |
| Room | 2.7.0 | ✅ Latest |

**Assessment: ✅ Well-maintained with consistent versions**

### 2.3 Build Performance

| Feature | Status | Notes |
|---------|--------|-------|
| Configuration Cache | ✅ Enabled | Reduces configuration time |
| Build Cache | ✅ Enabled | Reuses previous build outputs |
| Parallel Execution | ✅ Enabled | Builds independent modules concurrently |
| Build Profiling | ❌ Missing | No build time analysis |
| Build Scan | ❌ Missing | No remote build analytics |

---

## 3. Architecture Pattern Analysis

### 3.1 Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  :app (DashboardActivity)                                       │
│  :modules:* (ViewModels, Screens, UI)                           │
├─────────────────────────────────────────────────────────────────┤
│                    DOMAIN LAYER (Implicit) ⚠️                   │
│  :core:common (BaseViewModel, ModuleRegistry)                   │
│  - UiState, UiAction, UiEffect interfaces                       │
├─────────────────────────────────────────────────────────────────┤
│                    DATA LAYER                                   │
│  :core:datastore (DataStoreManager, EncryptedRoomFactory)       │
│  :core:proc (ProcReader)                                        │
│  :modules:* (Room DAOs, Databases)                              │
├─────────────────────────────────────────────────────────────────┤
│                    INFRASTRUCTURE                               │
│  Android Framework, SQLCipher, Ktor, Room, DataStore            │
└─────────────────────────────────────────────────────────────────┘
```

**Assessment: ⚠️ Domain layer is implicit** - Business logic could be more explicitly separated

### 3.2 Dependency Injection

**Hilt Configuration:**
- ✅ `@HiltAndroidApp` on `AlloyApplication`
- ✅ `@AndroidEntryPoint` on `DashboardActivity`
- ✅ `@HiltViewModel` on all ViewModels
- ✅ Proper module scoping (Singleton, ViewModelScoped)

**Assessment: ✅ Well-organized DI with proper scoping**

### 3.3 Inter-Module Communication

**Current Patterns:**
1. Shared core dependencies
2. ModuleRegistry for module state
3. Direct ViewModel injection via Hilt

**Missing:**
- ❌ Explicit interface contracts between modules
- ❌ Module-to-module API layer
- ❌ Navigation component for inter-module navigation

---

## 4. Critical Issues

### 4.1 🔴 HIGH SEVERITY

| Issue | Location | Impact | Recommendation |
|-------|----------|--------|----------------|
| **runBlocking in Production** | `core:datastore/EncryptedRoomFactory.kt:5` | Can cause ANR on main thread | Refactor to suspend function |

**Code Example:**
```kotlin
// ❌ CURRENT (Problematic)
fun getFactoryFor(dbName: String): SupportFactory {
    val passphraseBytes = runBlocking {
        dataStoreManager.setString("${dbName}_iv", ...)
        // ...
    }
    return SupportFactory(passphraseBytes)
}

// ✅ RECOMMENDED
suspend fun getFactoryFor(dbName: String): SupportFactory {
    val passphraseBytes = withContext(Dispatchers.IO) {
        dataStoreManager.setString("${dbName}_iv", ...)
        // ...
    }
    return SupportFactory(passphraseBytes)
}
```

### 4.2 🟠 MEDIUM SEVERITY

| Issue | Impact | Recommendation |
|-------|--------|----------------|
| **Hardcoded Module Registry** | Not extensible without rebuild | Consider config-driven registration |
| **No Module Interface Contracts** | Tight coupling between modules | Define module interfaces |
| **Implicit Navigation** | Hard to maintain navigation | Add Navigation Compose |
| **Missing Domain Layer** | Business logic mixed with presentation | Extract use cases to domain layer |

### 4.3 🟡 LOW SEVERITY

| Issue | Impact | Recommendation |
|-------|--------|----------------|
| **Duplicate Dependencies** | Larger APK, maintenance overhead | Use shared dependency BOM |
| **No Module Scaffolding** | Slow onboarding for new modules | Create Gradle plugin template |
| **Limited DFM Support** | Future technical debt | Implement DFM infrastructure now |

---

## 5. Extensibility Assessment

### 5.1 Adding New Feature Modules

**Current Process:**
```
1. Create module directory: modules/<name>/
2. Create build.gradle.kts with standard plugins
3. Add to settings.gradle.kts
4. Add to app's build.gradle.kts dependencies
5. Implement ViewModel, UI State, UI Screen, Database
```

**Assessment: ⚠️ Well-documented but fully manual**

### 5.2 Module Templates/Scaffolding

**Status: ❌ NO TEMPLATES FOUND**

- No Gradle plugin for module generation
- No ktlint/ktfmt templates
- No standard module structure enforced beyond convention

### 5.3 Module Registry/Discovery

**Current Implementation:**
```kotlin
private val registeredModulesList = listOf(
    ModuleInfo("statspill", "Stats Vitals", "...", "System"),
    ModuleInfo("scenes", "Workspace Scenes", "...", "Desktop"),
    // ...
)
```

**Assessment:**
- ✅ Type-safe (hardcoded)
- ✅ ModuleInfo provides metadata
- ⚠️ Requires code changes to add modules
- ⚠️ No discovery mechanism for DFM modules

---

## 6. Scalability Concerns

### 6.1 Potential Bottlenecks

| Bottleneck | Severity | Mitigation |
|------------|----------|------------|
| KSP Processing | Medium | Room DAOs generated per module |
| Module Compilation | Low | Independent module compilation |
| Dependency Resolution | Low | Version catalog mitigates this |

### 6.2 Future Growth Projections

| Scale | Modules | Assessment |
|-------|---------|------------|
| Current | 12 | ✅ Well-structured |
| Phase 1 (6 months) | 15-20 | ✅ Manageable |
| Phase 2 (12 months) | 25-30 | ⚠️ Consider DFM |
| Phase 3 (24 months) | 40+ | ⚠️ Need build optimization |

---

## 7. Recommendations

### 7.1 Immediate Actions (This Sprint)

| Priority | Action | Effort | Impact |
|----------|--------|--------|--------|
| 🔴 P0 | Fix runBlocking in EncryptedRoomFactory | 1h | High |
| 🟠 P1 | Add Navigation Compose | 4h | Medium |
| 🟠 P1 | Create Module Interface Contract | 2h | Medium |

### 7.2 Short-term Actions (Next Sprint)

| Priority | Action | Effort | Impact |
|----------|--------|--------|--------|
| 🟠 P1 | Extract Domain Layer (UseCases) | 8h | High |
| 🟡 P2 | Create Module Scaffolding Plugin | 12h | Medium |
| 🟡 P2 | Add Build Scan Configuration | 2h | Low |

### 7.3 Long-term Actions (Post-MVP)

| Priority | Action | Effort | Impact |
|----------|--------|--------|--------|
| 🟡 P2 | Implement DFM Infrastructure | 20h | High |
| 🟡 P2 | Add Integration Test Framework | 16h | Medium |
| 🟢 P3 | Build Performance Profiling | 8h | Medium |

---

## 8. Architecture Decision Records (ADRs)

### ADR-001: Module Structure

**Status:** Accepted  
**Date:** 2024-09-24

**Decision:** Use hierarchical module structure with clear separation between core and feature modules.

**Rationale:**
- Enables independent compilation
- Reduces build time for feature development
- Clear boundaries for testing
- Supports DFM in future

**Consequences:**
- Requires careful dependency management
- Module registration is manual
- Some code duplication across modules

### ADR-002: MVI Pattern

**Status:** Accepted  
**Date:** 2024-09-24

**Decision:** Use MVI (Model-View-Intent) pattern with StateFlow for UI state management.

**Rationale:**
- Unidirectional data flow
- Predictable state transitions
- Easy to test
- Compose-friendly

**Consequences:**
- More boilerplate than MVVM
- Requires careful effect management
- State objects can become large

### ADR-003: Hilt for DI

**Status:** Accepted  
**Date:** 2024-09-24

**Decision:** Use Hilt for dependency injection across all modules.

**Rationale:**
- Compile-time DI
- Android-optimized
- Industry standard
- Good tooling support

**Consequences:**
- Longer build times (KSP)
- Less flexibility than manual DI
- Tied to Android lifecycle

### ADR-004: SQLCipher Encryption

**Status:** Accepted  
**Date:** 2024-09-24

**Decision:** Use SQLCipher for encrypted Room databases.

**Rationale:**
- Industry-standard encryption
- Transparent to application
- Good performance
- Active maintenance

**Consequences:**
- Larger APK size
- Requires native libraries
- Passphrase management complexity

---

## 9. Extensibility Score Breakdown

| Factor | Score | Weight | Weighted Score |
|--------|-------|--------|----------------|
| Module Structure | 8/10 | 20% | 1.60 |
| Build Configuration | 8/10 | 15% | 1.20 |
| Architecture Patterns | 7/10 | 25% | 1.75 |
| Dependency Management | 7/10 | 15% | 1.05 |
| Documentation | 9/10 | 10% | 0.90 |
| Tooling (Lint, Testing) | 8/10 | 10% | 0.80 |
| DFM Readiness | 3/10 | 5% | 0.15 |
| **TOTAL** | | **100%** | **7.45/10** |

---

## 10. Action Items

### To Fix Before Feature Development

- [ ] 🔴 Fix runBlocking in EncryptedRoomFactory (P0)
- [ ] 🟠 Add Navigation Compose infrastructure (P1)
- [ ] 🟠 Create ModuleInterface base contract (P1)
- [ ] 🟠 Extract domain layer with use cases (P1)

### To Implement in Next Sprint

- [ ] 🟡 Create module scaffolding Gradle plugin (P2)
- [ ] 🟡 Add Build Scan configuration (P2)
- [ ] 🟡 Add integration test framework (P2)
- [ ] 🟢 Profile build times and optimize (P3)

### Long-term Planning

- [ ] 🟢 Implement DFM infrastructure (Post-MVP)
- [ ] 🟢 Add performance testing pipeline (Post-MVP)
- [ ] 🟢 Create module marketplace/discovery (Post-MVP)

---

## 11. Conclusion

The Alloy project demonstrates a **strong architectural foundation** with clear adherence to modern Android development practices. The modular structure, comprehensive lint rules, and well-documented patterns position it well for scaling.

**Key Strengths:**
- ✅ Clean module boundaries with no circular dependencies
- ✅ Excellent documentation and code quality enforcement
- ✅ Modern tech stack (Kotlin 2.0, Compose, Hilt)
- ✅ Good build optimization settings

**Key Weaknesses:**
- ⚠️ Critical: runBlocking in production code
- ⚠️ Missing domain layer for business logic
- ⚠️ No DFM infrastructure for on-demand delivery
- ⚠️ Manual module registration process

**Overall Assessment:** The project is **well-positioned for its stated goals** and demonstrates senior-level architectural decision-making in most areas. The identified issues are primarily improvements rather than blockers, with one critical fix required before proceeding.

---

**Next Review:** After implementing immediate fixes (runBlocking, Navigation, Domain Layer)  
**Review Frequency:** Quarterly  
**Owner:** Architecture Team
