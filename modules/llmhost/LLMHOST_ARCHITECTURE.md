# LLM Host Modular Architecture

This document describes the modular, extensible Clean Architecture and MVI implementation for the LLM Host module, maintaining parity with the architectural standards of the `statspill` reference module.

## Overview

The LLM Host system is organized into distinct, unidirectional architectural layers:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (MVI)                                   │
│  LlmHostScreen → LlmHostUiState ←→ LlmHostUiAction → LlmHostViewModel  │
│  ServerStatusCard / ModelManagerCard / StorageInfoCard / ModelTestCard   │
└─────────────────────────────────────────────────────────────────────────┘
                                      ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER (Use Cases & Contracts)                │
│  ObserveHostStateUseCase, StartHostServerUseCase, DownloadModelUseCase   │
│  RunTestInferenceUseCase, DeleteModelUseCase, GetModelStorageUseCase...  │
│  ILlmHostRepository (Contract Interface)                                 │
│  Domain Models: InstalledModel, DownloadProgress, StorageUsage...       │
└─────────────────────────────────────────────────────────────────────────┘
                                      ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    DATA LAYER (Sources & Coordinators)                  │
│  LlmHostRepositoryImpl (Coordinates engine, server, and storage)        │
│  ILlmEngine (LiteRtLlmEngine / MockLlmEngine)                            │
│  ModelDownloader (Streaming OkHttp + HF Auth), ModelStorageManager       │
│  LlmHttpServer (Embedded Ktor CIO loopback server)                       │
└─────────────────────────────────────────────────────────────────────────┘
                                      ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE & SYSTEM (Android APIs)                 │
│  Google AI Edge LiteRT-LM runtime, StatFs disk stats, Android Keystore  │
│  LlmHostServerService (Android Foreground Service)                      │
└─────────────────────────────────────────────────────────────────────────┘
```

## Core Principles

### 1. Unidirectional Data Flow (MVI)
- **`LlmHostUiState`**: Immutable pure data state representing everything the UI needs. Free from Android framework leaks (`Intent`, `Context`).
- **`LlmHostUiAction`**: Sealed interface describing all user interactions.
- **`LlmHostUiEffect`**: One-shot channel-backed side effects collected via `LaunchedEffect(Unit)`.

### 2. Clean Architecture & Focused Use Cases
- Business logic and download/inference coordination reside in the Domain layer (`ObserveHostStateUseCase`, `DownloadModelUseCase`, `RunTestInferenceUseCase`, etc.).
- UI components and ViewModels depend on Use Cases rather than coupling directly to concrete data sources or network clients.
- The background service (`LlmHostServerService`) coordinates with domain repository streams to keep the server alive across desktop multitasking.

### 3. LiteRT-LM & Memory Safety on 16 GB Googlebooks
- Engine abstraction (`ILlmEngine`) decouples UI and API serving from the native runtime.
- Model loading is sequential with a mutex, ensuring KV-cache memory stability.
- Clean shutdown (`close()`) cleans native pointers to prevent memory leaks.
- Storage queries use `StatFs` on app-private storage, preventing external storage permission overhead.

### 4. Normalized Packages & Isolation
All files belonging to the LLM Host module are located under the root namespace:
`com.squidink.alloy.modules.llmhost.*`
