---
name: local-delegation
description: >-
  Delegates simple, repetitive, or boilerplate coding tasks to the local Qwen3.5-122B model
  (unsloth/Qwen3.5-122B-A10B-NVFP4 via vLLM) and offloads developer/web research to the local
  SearXNG search instance. Use when generating boilerplate, utility functions, regexes, docstrings,
  test cases, or performing technical web searches.
---

# Local Delegation Skill: Qwen3.5-122B & SearXNG

This skill provides operational runbooks, heuristics, and tools to offload simple tasks and research queries to your local cluster:
- **Local LLM**: `Qwen3.5-122B-A10B-NVFP4` at `http://gx10.access.net:8080/coding/v1`
- **Local Search**: SearXNG at `http://gx10.access.net:8080/coding/search`

---

## 1. Delegation Heuristics: When to Offload

### ✅ Tasks Ideal for Local Qwen 3.5:
1. **Boilerplate Code**:
   - Creating Kotlin data classes, DTOs, entity mappers, and Parcelable/Serializable models.
   - Writing standard Room DAOs or TypeConverters.
   - Standard equals/hashCode/toString/copy implementations.
2. **Pure Utility Functions**:
   - String formatting, date parsing, byte size converters, color conversions.
   - Regex patterns for email, URL, phone number, or path validation.
   - Standard algorithms (sorting, binary search, tree traversals, math calculations).
3. **Documentation & Tests**:
   - Drafting KDocs / Javadocs for existing functions and classes.
   - Generating JUnit test cases, edge cases, and parameterized test datasets.
4. **Data Transformations**:
   - Converting JSON schemas to Kotlin models or vice versa.
   - Generating shell/bash one-liners.

### ⛔ Tasks to Keep on Cloud Antigravity:
1. **Architectural & Multi-file Planning**: Defining system boundaries, module dependencies, or Clean Architecture refactors.
2. **Interactive Editing & Tool Execution**: Modifying codebase files with `replace_file_content` and verifying with `./gradlew build`.
3. **Complex Bug Investigations**: Tracing multi-layer stack traces and SELinux/OS permission discrepancies.

### 🔍 Research Ideal for Local SearXNG:
- Finding Kotlin, Android Jetpack, or library API documentation.
- Searching GitHub repos, StackOverflow questions, and release notes for specific error strings.
- Discovering recommended Maven/Gradle coordinates.

---

## 2. Invocation Methods

### Method A: Via MCP Tools (Recommended)
When the MCP server is mounted, Antigravity has native access to:

1. **`ask_local_qwen`**:
   ```json
   {
     "prompt": "Write a Kotlin extension function on Long to format bytes to human-readable string (KB, MB, GB).",
     "system_prompt": "Write clean, idiomatic Kotlin code without external libraries.",
     "max_tokens": 1024,
     "temperature": 0.5
   }
   ```
   *Note*: The tool strips the internal `<thinking>` chain by default to preserve tokens and response cleanliness. Pass `"include_reasoning": true` if you wish to inspect the model's thoughts.

2. **`searxng_search`**:
   ```json
   {
     "query": "Android Jetpack Compose NavigationRail padding",
     "categories": "it,general",
     "max_results": 5
   }
   ```

### Method B: Via Standalone CLI Scripts
If you prefer running terminal commands or piping data directly:

1. **Query Qwen from Terminal**:
   ```bash
   python3 .agents/skills/local-delegation/scripts/qwen_client.py "Write a regex to match IPv4 addresses"
   ```
   Or pipe context:
   ```bash
   cat schema.json | python3 .agents/skills/local-delegation/scripts/qwen_client.py "Generate Kotlin data classes for this JSON"
   ```

2. **Query SearXNG from Terminal**:
   ```bash
   python3 .agents/skills/local-delegation/scripts/searxng_client.py -n 5 "Kotlin Coroutines Flow debounce"
   ```

---

## 3. Best Practices for Qwen 3.5 Prompting
- **Always provide a clear system prompt**: e.g., `"Write modern, idiomatic Kotlin using Jetpack Compose and Coroutines."`
- **Adequate `max_tokens`**: The default limit is 32768 tokens (supporting up to 65536) to give the model ample headroom for internal reasoning chains (<thinking>) while generating complete multi-file implementations without choking.
