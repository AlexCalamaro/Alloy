# LLM Host Module

On-device LiteRT Large Language Model host exposing local loopback OpenAI-compatible REST endpoints for Googlebook OS applications.

## Overview

The LLM Host module (`modules/llmhost`) runs on-device Large Language Models using Google AI Edge's **LiteRT-LM** engine. Built specifically for **Googlebook OS** (16 GB unified RAM compact laptops, Android 17 / API 37), the module operates as a headless hosting service allowing users to download `.litertlm` models from Hugging Face and expose OpenAI-compatible REST endpoints (`127.0.0.1:<port>`) with bearer token authentication for local developer tools, scripts, and applications.

## Capabilities

### Core Features

- **LiteRT-LM On-Device Engine**
  - High-performance, hardware-accelerated LLM runtime (`com.google.ai.edge.litertlm:litertlm-android`).
  - Supports GPU (OpenCL / Vulkan) and CPU (ARM NEON / AVX2) backends.
  - Efficient KV-cache management and low thermal draw on compact laptops.

- **Hugging Face Model Downloader**
  - Download custom `.litertlm` models directly from Hugging Face URLs.
  - Optional Hugging Face Access Token support for gated models (such as Gemma).
  - Streaming download with real-time progress (percentage, downloaded MB, total MB, transfer speed).
  - Resilient downloads with cancellation and atomic file placement.

- **Localhost HTTP Server (OpenAI Compatible)**
  - Embedded Ktor CIO server bound strictly to `127.0.0.1` (loopback only) on a configurable port (default `8787`).
  - Bearer token authentication to secure local API access.
  - Standard endpoints:
    - `GET /v1/health` — Status and model telemetry.
    - `GET /v1/models` — OpenAI models list.
    - `POST /v1/chat/completions` — Standard multi-turn chat completions.
    - `POST /v1/completions` — Legacy prompt completions.
  - Backed by an Android Foreground Service (`LlmHostServerService`) so the server remains active when other desktop apps are focused.

- **Diagnostic Model Test Runner**
  - Dedicated prompt runner to verify model output without an interactive chat interface.
  - Measures latency (ms), token count, and generation throughput (tokens/second).
  - Quick preset chips for rapid validation.

- **Storage & Disk Utilization**
  - Displays current model size, total models directory footprint, and device free storage via `StatFs`.
  - Delete model capability with confirmation dialog to free disk space.

- **Curated Googlebook OS Model Recommendations**
  - Tailored specifically for 16 GB RAM compact Googlebook laptops:
    - **Gemma 2 2B IT** (~1.5 GB): Snappy responses with minimal battery and thermal draw.
    - **Gemma 2 9B IT** (~5.4 GB): Desktop-grade reasoning fitting comfortably within the 16 GB RAM budget.
    - **Phi-3.5 Mini Instruct** (~2.2 GB): Strong math and logic capabilities.
    - **Llama 3.2 1B Instruct** (~1.1 GB): Ultra-compact helper model.
  - One-click "Use Model URL" preset population.

---

## Architecture

The module adheres strictly to **Clean Architecture** and **Model-View-Intent (MVI)** principles matching `modules/statspill`:

```
modules/llmhost/
├── build.gradle.kts
├── README.md
├── LLMHOST_ARCHITECTURE.md
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/squidink/alloy/modules/llmhost/
│       ├── LlmHostViewModel.kt               # Pure MVI ViewModel
│       ├── LlmHostActionsImpl.kt             # Cross-module ILlmHostActions provider
│       ├── LlmHostFeatureDetail.kt           # Alloy FeatureDetail integration
│       ├── di/
│       │   └── LlmHostModule.kt              # Hilt interface & provider bindings
│       ├── service/
│       │   ├── LlmHostServerService.kt       # Persistent Foreground Service
│       │   └── LlmHostServiceManager.kt      # Service intent lifecycle manager
│       ├── domain/
│       │   ├── model/
│       │   │   ├── LlmHostModels.kt          # HostState, InstalledModel, DownloadProgress...
│       │   │   ├── RecommendedModel.kt       # Curated LiteRT models & Googlebook specs
│       │   │   ├── InferenceResult.kt        # Test prompt result & token metrics
│       │   │   └── LlmHostError.kt           # Typed domain errors
│       │   ├── repository/
│       │   │   └── ILlmHostRepository.kt     # Domain repository contract
│       │   └── usecase/
│       │       ├── ObserveHostStateUseCase.kt
│       │       ├── StartHostServerUseCase.kt
│       │       ├── StopHostServerUseCase.kt
│       │       ├── DownloadModelUseCase.kt
│       │       ├── CancelDownloadUseCase.kt
│       │       ├── DeleteModelUseCase.kt
│       │       ├── RunTestInferenceUseCase.kt
│       │       ├── GetModelStorageUseCase.kt
│       │       ├── GetRecommendedModelsUseCase.kt
│       │       └── ObserveErrorsUseCase.kt
│       ├── data/
│       │   ├── LlmHostRepositoryImpl.kt      # Repository implementation
│       │   ├── datasource/
│       │   │   ├── engine/
│       │   │   │   ├── ILlmEngine.kt         # Engine interface abstraction
│       │   │   │   ├── LiteRtLlmEngine.kt    # Production LiteRT-LM runtime
│       │   │   │   └── MockLlmEngine.kt      # Test & emulator mock engine
│       │   │   ├── server/
│       │   │   │   ├── LlmHttpServer.kt      # Ktor CIO 127.0.0.1 OpenAI server
│       │   │   │   └── OpenAIContracts.kt   # Request/response DTOs
│       │   │   └── downloader/
│       │   │       ├── ModelDownloader.kt    # OkHttp streaming downloader with HF auth
│       │   │       └── ModelStorageManager.kt # File & StatFs storage calculations
│       └── ui/
│           ├── LlmHostScreen.kt              # Main dashboard composable
│           ├── LlmHostSettingsPanel.kt       # Settings side sheet
│           └── components/
│               ├── ServerStatusCard.kt       # Toggle, address, token, status
│               ├── ModelManagerCard.kt       # HF URL/token inputs, progress bar
│               ├── StorageInfoCard.kt        # Model storage, disk free, delete action
│               ├── ModelTestCard.kt          # Diagnostic prompt runner & metrics
│               └── ModelRecommendationsCard.kt # Curated Googlebook LiteRT cards
└── src/test/java/com/squidink/alloy/modules/llmhost/
    ├── LlmHostViewModelTest.kt               # Exhaustive MVI ViewModel unit tests
    ├── FakeLlmHostRepository.kt              # Test double repository
    ├── data/
    │   ├── LlmHostRepositoryImplTest.kt      # Repository coordination tests
    │   ├── ModelDownloaderTest.kt            # Streaming download & auth tests
    │   └── LlmHttpServerTest.kt              # Ktor server route & auth tests
    └── domain/usecase/
        └── LlmHostUseCasesTest.kt            # Domain use cases execution tests
```

---

## Testing

Run all unit tests:
```bash
./gradlew :modules:llmhost:testDebugUnitTest
```
