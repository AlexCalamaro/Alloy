# Alloy Android Project - Work Plan

**Last Updated:** September 24, 2026  
**Status:** Architecture Review Complete, Ready for Feature Development

---

## Project Overview

Alloy is a modular Android desktop utility suite (similar to Microsoft PowerToys) targeting Android 17 (API 37). The project emphasizes clean architecture, comprehensive testing, and strict code quality enforcement.

### Technology Stack

| Component | Version | Status |
|-----------|---------|--------|
| Kotlin | 2.0.21 | ✅ Latest |
| Android Gradle Plugin | 9.4.1 | ✅ Latest |
| Compose BOM | 2025.02.00 | ✅ Recent |
| Hilt | 2.60.1 | ✅ Stable |
| Room | 2.7.0 | ✅ Latest |
| Coroutines | 1.10.1 | ✅ Latest |
| Ktor | 3.1.0 | ✅ Latest |

### Module Structure

```
:app (Main Application)
├── :core:common (DI, Events, Registry)
├── :core:design (Material 3 Theme)
├── :core:datastore (Room/SQLCipher)
├── :core:proc (System Telemetry)
├── :core:netlocal (Ktor Loopback)
├── :modules:statspill (System Widget)
├── :modules:scenes (Workspace Launcher)
├── :modules:clip (Clipboard History)
├── :modules:scratch (Scratchpad)
├── :tooling:probe (Platform Validation)
└── :lint (Custom Lint Rules)
```

---

## Phase 1: Foundation (Current Sprint)

### 1.1 Build System Setup ✅ COMPLETED

- [x] Gradle configuration with parallel execution
- [x] Build cache and configuration cache enabled
- [x] Version catalog (libs.versions.toml)
- [x] Plugin aliases for all dependencies

### 1.2 Core Infrastructure ✅ COMPLETED

- [x] `:core:common` - BaseViewModel, ModuleRegistry, EventFlow
- [x] `:core:design` - Material 3 theme, desktop UX primitives
- [x] `:core:datastore` - DataStoreManager, EncryptedRoomFactory
- [x] `:core:proc` - ProcReader for /proc telemetry
- [x] `:core:netlocal` - Ktor loopback server

### 1.3 Feature Modules ✅ COMPLETED

- [x] `:modules:statspill` - System stats widget and overlay
- [x] `:modules:scenes` - Workspace scene launcher
- [x] `:modules:clip` - Encrypted clipboard history
- [x] `:modules:scratch` - Multi-instance scratchpad

### 1.4 Quality Enforcement ✅ COMPLETED

- [x] Detekt configuration (detekt.yml)
- [x] Custom lint rules module (:lint)
- [x] Pre-commit hooks (pre-commit.sh)
- [x] Gradle lint configuration
- [x] Build warnings-as-errors policy

---

## Phase 2: Architecture Improvements (Next Sprint)

### 2.1 Critical Fixes ✅ COMPLETED

- [x] **Fix runBlocking in EncryptedRoomFactory** - Converted to suspend function with Dispatchers.IO
- [x] **Fix ScratchModule security** - Migrated from hardcoded passphrase to EncryptedRoomFactory
- [x] **Update ClipModule** - Added runBlocking wrapper for Hilt provider

### 2.2 Navigation Infrastructure 🔄 IN PROGRESS

- [ ] Add Navigation Compose dependency
- [ ] Create navigation graph
- [ ] Implement type-safe navigation arguments
- [ ] Add deep link support for modules

### 2.3 Domain Layer Extraction 📋 PLANNED

- [ ] Create `:core:domain` module
- [ ] Extract use cases from ViewModels
- [ ] Define repository interfaces
- [ ] Move business logic to domain layer

### 2.4 Module Interface Contracts 📋 PLANNED

- [ ] Define `ModuleContract` interface
- [ ] Implement in all feature modules
- [ ] Add module lifecycle callbacks
- [ ] Create module-to-module communication API

---

## Phase 3: Extensibility Enhancements (Future Sprints)

### 3.1 Module Scaffolding

- [ ] Create Gradle plugin for module generation
- [ ] Add module template with tests
- [ ] Document scaffolding process
- [ ] Add ktlint/ktfmt templates

### 3.2 Dynamic Feature Modules

- [ ] Implement DFM infrastructure
- [ ] Create on-demand delivery configuration
- [ ] Implement module loading mechanism
- [ ] Add module marketplace/discovery

### 3.3 Build Optimization

- [ ] Add Build Scan configuration
- [ ] Implement dependency analysis
- [ ] Add build profile separation (debug/release)
- [ ] Profile and optimize build times

---

## Phase 4: Testing Infrastructure (Post-MVP)

### 4.1 Integration Testing

- [ ] Create integration test module
- [ ] Set up UI test framework
- [ ] Add end-to-end test scenarios
- [ ] Implement test data builders

### 4.2 Performance Testing

- [ ] Add performance testing pipeline
- [ ] Set up benchmark tests
- [ ] Implement memory leak detection
- [ ] Add startup time measurements

### 4.3 CI/CD Pipeline

- [ ] Configure GitHub Actions / GitLab CI
- [ ] Add automated build on PR
- [ ] Add automated test execution
- [ ] Add deployment pipeline

---

## Current Sprint Tasks

### Priority: P0 (Critical)

| Task | Status | Owner | Notes |
|------|--------|-------|-------|
| Fix runBlocking in EncryptedRoomFactory | ✅ Done | Architecture Team | Converted to suspend function |
| Fix ScratchModule security | ✅ Done | Security Team | Migrated to EncryptedRoomFactory |
| Update ClipModule wrapper | ✅ Done | Architecture Team | Added runBlocking(Dispatchers.IO) |

### Priority: P1 (High)

| Task | Status | Owner | Notes |
|------|--------|-------|-------|
| Add Navigation Compose | 🔄 In Progress | UI Team | Create navigation graph |
| Create ModuleInterface contract | 📋 Planned | Architecture Team | Define module API |
| Extract domain layer | 📋 Planned | Architecture Team | Move business logic |

### Priority: P2 (Medium)

| Task | Status | Owner | Notes |
|------|--------|-------|-------|
| Create module scaffolding plugin | 📋 Planned | DevEx Team | Gradle plugin for new modules |
| Add Build Scan configuration | 📋 Planned | DevEx Team | Build analytics |
| Add integration test framework | 📋 Planned | QA Team | E2E testing |

---

## Architecture Review Findings

### Extensibility Score: 7/10

**Strengths:**
- ✅ Clean module boundaries with no circular dependencies
- ✅ Excellent documentation and code quality enforcement
- ✅ Modern tech stack (Kotlin 2.0, Compose, Hilt)
- ✅ Good build optimization settings

**Weaknesses:**
- ⚠️ Missing domain layer for business logic
- ⚠️ No DFM infrastructure for on-demand delivery
- ⚠️ Manual module registration process
- ⚠️ No module scaffolding tooling

### Build Structure Assessment: B+ (85/100)

| Category | Score | Notes |
|----------|-------|-------|
| Gradle Configuration | 90 | Excellent optimization settings |
| Dependency Management | 85 | Version catalog, some duplication |
| Module Organization | 90 | Clean separation, no cycles |
| Build Performance | 80 | Good, but no build profiling |
| Code Quality | 95 | Custom lint, detekt configured |
| Testing Infrastructure | 75 | Unit tests present, integration missing |

---

## Known Issues

### 🔴 Critical (Must Fix Before Production)

| Issue | Module | Status |
|-------|--------|--------|
| ~~runBlocking in EncryptedRoomFactory~~ | ~~core:datastore~~ | ~~Fixed~~ |
| ~~Hardcoded passphrase in ScratchModule~~ | ~~modules:scratch~~ | ~~Fixed~~ |

### 🟠 High (Should Fix in Next Sprint)

| Issue | Module | Status |
|-------|--------|--------|
| Missing domain layer | All | Planned |
| No navigation component | app | In Progress |
| No module interface contracts | All | Planned |

### 🟡 Medium (Plan for Upcoming Sprints)

| Issue | Module | Status |
|-------|--------|--------|
| Duplicate dependencies | All | Low Priority |
| No module scaffolding | Build | Planned |
| Limited DFM support | Build | Future |

---

## Release Milestones

| Milestone | Target | Features | Status |
|-----------|--------|----------|--------|
| MVP Alpha | 2024-10-01 | StatsPill, Scenes | 🟡 In Development |
| MVP Beta | 2024-10-15 | + Clip, Scratch | ⚪ Planned |
| MVP Release | 2024-11-01 | All features, testing | ⚪ Planned |
| v1.1 | 2024-12-01 | Performance, DFM | ⚪ Planned |
| v2.0 | 2025-02-01 | New modules, marketplace | ⚪ Planned |

---

## Documentation

### Architectural Documents

| Document | Location | Status |
|----------|----------|--------|
| Architecture Review | `.md-storage/audit-reports/2026-09-24-macro-architecture-review.md` | ✅ Complete |
| Architecture Decision Records | `.md-storage/audit-reports/2026-09-24-architecture-decision-records.md` | ✅ Complete |
| Antipatterns Report | `.md-storage/audit-reports/2026-09-24-antipatterns-audit.md` | ✅ Complete |
| Code Quality Summary | `.md-storage/summaries/2026-09-24-code-quality-summary-updated.md` | ✅ Complete |

### Best Practices

| Document | Location | Status |
|----------|----------|--------|
| Android Best Practices | `ANDROID_BEST_PRACTICES.md` | ✅ Active |
| Third-Party Licenses | `third-party-licenses.md` | ✅ Generated |

### Scripts

| Script | Purpose | Status |
|--------|---------|--------|
| `scripts/check-licenses.sh` | License compliance | ✅ Active |
| `scripts/cleanup-md-storage.sh` | MD file lifecycle | ✅ Active |
| `scripts/move-to-storage.sh` | Move MD files | ✅ Active |

---

## Team Guidelines

### Code Review Checklist

- [ ] No GlobalScope or unconfined coroutines
- [ ] State hoisted to parent/ViewModel
- [ ] LazyColumn/LazyRow has key function
- [ ] Error handling on all suspending operations
- [ ] KDoc on public APIs
- [ ] No magic numbers (use constants)
- [ ] Proper lifecycle management
- [ ] No context leaks
- [ ] No build warnings (or well-justified suppressions)
- [ ] All @Suppress annotations have comments explaining why
- [ ] New dependencies have approved permissive licenses
- [ ] No generated markdown files in root directory

### Commit Guidelines

1. **Pre-commit Checks**: Run `./gradlew detekt lintKotlin`
2. **License Check**: Run `./scripts/check-licenses.sh` for new deps
3. **Test**: Run `./gradlew testDebugUnitTest`
4. **Message Format**: `type(scope): description`
   - Example: `feat(clip): add pinning functionality`

### Adding New Modules

1. Create module directory with standard structure
2. Add to `settings.gradle.kts`
3. Add dependencies to `app/build.gradle.kts`
4. Register in `ModuleRegistryImpl`
5. Add KDoc to all public APIs
6. Run `./scripts/check-licenses.sh`

---

## Metrics & KPIs

### Quality Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Build Time | < 5 min | ~47s (incremental) |
| Test Coverage | 80% | ~60% |
| Detekt Violations | 0 | 0 |
| Lint Errors | 0 | 0 |
| Critical Issues | 0 | 0 |

### Performance Metrics

| Metric | Target | Current |
|--------|--------|---------|
| App Startup | < 500ms | TBD |
| Module Load Time | < 100ms | TBD |
| Memory Usage | < 100MB | TBD |

---

## Next Steps

1. ✅ Architecture review complete
2. ✅ Critical fixes applied (runBlocking, security)
3. ⏳ Add Navigation Compose infrastructure
4. ⏳ Create ModuleInterface contract
5. ⏳ Extract domain layer
6. ⏳ Begin feature development

---

**Document Owner:** Architecture Team  
**Review Frequency:** Weekly  
**Last Reviewed:** September 24, 2026
