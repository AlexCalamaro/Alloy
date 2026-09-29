# Local Delegation Guidelines

When fulfilling user requests, leverage the local compute and search cluster:

## Local Model Delegation (Qwen3.5-122B)
- **When Available**: The local model `unsloth/Qwen3.5-122B-A10B-NVFP4` is available at `http://gx10.access.net:8080/coding/v1` via the `ask_local_qwen` MCP tool or `.agents/skills/local-delegation/scripts/qwen_client.py`.
- **Ideal Candidates for Offloading**:
  - Boilerplate data classes, DTOs, mappers, parsers, and serializable models.
  - Pure utility functions (formatting, date/time, string manipulation, conversions).
  - Regular expressions and shell/bash one-liners.
  - Unit test cases, parameter tables, and edge case assertions.
  - Generating documentation comments (KDocs, Javadocs).
- **Quality Control**: Always review and verify output from the local model before integrating it into project files or submitting it to the user.

## Local Web Research (SearXNG)
- **When Available**: The local SearXNG search instance is available at `http://gx10.access.net:8080/coding/search` via the `searxng_search` MCP tool or `.agents/skills/local-delegation/scripts/searxng_client.py`.
- **Search Strategy**: Prioritize `searxng_search` for developer documentation, library references, GitHub repositories, and StackOverflow solutions to minimize latency and external tracking.
