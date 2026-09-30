#!/usr/bin/env python3
"""
Local Cluster MCP Server
Exposes local Qwen3.5-122B (vLLM) and SearXNG search engine as native MCP tools
for Google Antigravity and subagents.
"""

import sys
import json
import urllib.request
import urllib.parse
import urllib.error

QWEN_API_URL = "http://gx10.access.net:8080/coding/v1/chat/completions"
QWEN_MODEL_ID = "unsloth/Qwen3.5-122B-A10B-NVFP4"
SEARXNG_URL = "http://gx10.access.net:8080/coding/search"


def query_qwen(prompt, system_prompt=None, max_tokens=32768, temperature=0.6, include_reasoning=False):
    """
    Streams from local vLLM endpoint, collects thinking tokens and content tokens,
    and returns the formatted output. Streaming prevents HTTP read timeouts.
    """
    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    payload = {
        "model": QWEN_MODEL_ID,
        "messages": messages,
        "max_tokens": max(max_tokens, 512),
        "temperature": temperature,
        "presence_penalty": 0.1,
        "stream": True,
    }

    req = urllib.request.Request(
        QWEN_API_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )

    reasoning_parts = []
    content_parts = []

    try:
        # 300s timeout for long generations and deep reasoning chains
        with urllib.request.urlopen(req, timeout=300) as resp:
            for line in resp:
                line = line.decode("utf-8").strip()
                if not line or line == "data: [DONE]":
                    continue
                if line.startswith("data: "):
                    try:
                        chunk = json.loads(line[6:])
                        choices = chunk.get("choices", [])
                        if not choices:
                            continue
                        delta = choices[0].get("delta", {})
                        if "reasoning" in delta and delta["reasoning"]:
                            reasoning_parts.append(delta["reasoning"])
                        if "content" in delta and delta["content"]:
                            content_parts.append(delta["content"])
                    except json.JSONDecodeError:
                        continue
    except urllib.error.URLError as e:
        return f"Error contacting local Qwen model at {QWEN_API_URL}: {e}"
    except Exception as e:
        return f"Unexpected error during local Qwen inference: {e}"

    final_content = "".join(content_parts).strip()
    reasoning_text = "".join(reasoning_parts).strip()

    if not final_content and reasoning_text:
        # Model might have hit max_tokens during reasoning
        return f"*(Model generated reasoning but reached max_tokens before completing content)*\n\n```thinking\n{reasoning_text}\n```"

    if include_reasoning and reasoning_text:
        return f"<thinking>\n{reasoning_text}\n</thinking>\n\n{final_content}"

    return final_content


def query_searxng(query, categories="it,general", engines=None, max_results=8):
    """
    Queries local SearXNG instance and returns formatted markdown search results.
    """
    params = {
        "q": query,
        "format": "json",
    }
    if categories:
        params["categories"] = categories
    if engines:
        params["engines"] = engines

    url = f"{SEARXNG_URL}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "AntigravityLocalDelegation/1.0"}
    )

    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except urllib.error.URLError as e:
        return f"Error querying local SearXNG at {SEARXNG_URL}: {e}"
    except Exception as e:
        return f"Unexpected error during SearXNG query: {e}"

    results = data.get("results", [])
    if not results:
        return f"No results found on SearXNG for query: '{query}'"

    lines = [f"### SearXNG Results for: '{query}'\n"]
    for i, item in enumerate(results[:max_results], 1):
        title = item.get("title", "Untitled")
        item_url = item.get("url", "")
        content = item.get("content", "").strip()
        engine = item.get("engine", "")

        engine_badge = f" `[{engine}]`" if engine else ""
        lines.append(f"{i}. **[{title}]({item_url})**{engine_badge}")
        if content:
            clean_content = " ".join(content.split())
            lines.append(f"   {clean_content}")
        lines.append("")

    return "\n".join(lines).strip()


TOOLS = [
    {
        "name": "ask_local_qwen",
        "description": (
            "Delegates coding, refactoring, boilerplate generation, regex, and reasoning tasks "
            "to the local Qwen3.5-122B model (unsloth/Qwen3.5-122B-A10B-NVFP4 on vLLM). "
            "Streams tokens with activity heartbeats to handle deep reasoning and long generations."
        ),
        "inputSchema": {
            "type": "object",
            "properties": {
                "prompt": {
                    "type": "string",
                    "description": "The instruction or coding prompt for Qwen3.5."
                },
                "system_prompt": {
                    "type": "string",
                    "description": "Optional system prompt defining role, style, or constraints (e.g. 'Write idiomatic Kotlin')."
                },
                "max_tokens": {
                    "type": "integer",
                    "description": "Maximum tokens to generate (default: 32768, supports up to 65536).",
                    "default": 32768
                },
                "temperature": {
                    "type": "number",
                    "description": "Sampling temperature between 0.0 and 1.0 (default: 0.6).",
                    "default": 0.6
                },
                "include_reasoning": {
                    "type": "boolean",
                    "description": "If true, includes the model's internal <thinking> reasoning trace in the response. Defaults to false.",
                    "default": False
                }
            },
            "required": ["prompt"]
        }
    },
    {
        "name": "searxng_search",
        "description": (
            "Performs web and documentation searches using the local SearXNG search engine instance. "
            "Returns ranked search results with titles, URLs, and snippets from enabled engines "
            "(Google, DuckDuckGo, Brave, GitHub, StackOverflow, Wikipedia)."
        ),
        "inputSchema": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "The search query string."
                },
                "categories": {
                    "type": "string",
                    "description": "Comma-separated search categories (e.g. 'it,general', 'science'). Defaults to 'it,general'.",
                    "default": "it,general"
                },
                "engines": {
                    "type": "string",
                    "description": "Optional comma-separated list of specific engines to query (e.g. 'github,stackoverflow')."
                },
                "max_results": {
                    "type": "integer",
                    "description": "Maximum number of results to return (default: 8).",
                    "default": 8
                }
            },
            "required": ["query"]
        }
    }
]


def send_response(response_dict):
    """Writes a single JSON-RPC line to stdout and flushes."""
    out = json.dumps(response_dict)
    sys.stdout.write(out + "\n")
    sys.stdout.flush()


def handle_request(req):
    """Processes a single JSON-RPC 2.0 request."""
    req_id = req.get("id")
    method = req.get("method")
    params = req.get("params", {})

    if method == "initialize":
        send_response({
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {}
                },
                "serverInfo": {
                    "name": "local-cluster",
                    "version": "1.0.0"
                }
            }
        })
    elif method == "notifications/initialized":
        # Notification, no response required
        pass
    elif method == "ping":
        send_response({
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {}
        })
    elif method == "tools/list":
        send_response({
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "tools": TOOLS
            }
        })
    elif method == "tools/call":
        tool_name = params.get("name")
        args = params.get("arguments", {})

        if tool_name == "ask_local_qwen":
            prompt = args.get("prompt", "")
            system_prompt = args.get("system_prompt")
            max_tokens = args.get("max_tokens", 32768)
            temperature = args.get("temperature", 0.6)
            include_reasoning = args.get("include_reasoning", False)

            result_text = query_qwen(
                prompt=prompt,
                system_prompt=system_prompt,
                max_tokens=max_tokens,
                temperature=temperature,
                include_reasoning=include_reasoning
            )
            send_response({
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": result_text
                        }
                    ]
                }
            })
        elif tool_name == "searxng_search":
            query = args.get("query", "")
            categories = args.get("categories", "it,general")
            engines = args.get("engines")
            max_results = args.get("max_results", 8)

            result_text = query_searxng(
                query=query,
                categories=categories,
                engines=engines,
                max_results=max_results
            )
            send_response({
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": result_text
                        }
                    ]
                }
            })
        else:
            send_response({
                "jsonrpc": "2.0",
                "id": req_id,
                "error": {
                    "code": -32601,
                    "message": f"Unknown tool: {tool_name}"
                }
            })
    else:
        if req_id is not None:
            send_response({
                "jsonrpc": "2.0",
                "id": req_id,
                "error": {
                    "code": -32601,
                    "message": f"Method not supported: {method}"
                }
            })


def main():
    """Main JSON-RPC stdio event loop."""
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
            handle_request(req)
        except json.JSONDecodeError as e:
            sys.stderr.write(f"Invalid JSON received on stdin: {e}\n")
            sys.stderr.flush()
        except Exception as e:
            sys.stderr.write(f"Error handling MCP request: {e}\n")
            sys.stderr.flush()


if __name__ == "__main__":
    main()
