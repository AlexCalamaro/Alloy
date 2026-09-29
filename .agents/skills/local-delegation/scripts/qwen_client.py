#!/usr/bin/env python3
"""
CLI Client for querying local Qwen3.5-122B model.
Usage:
    python3 qwen_client.py "Write a Kotlin data class for User"
    echo "Explain this code" | python3 qwen_client.py
"""

import sys
import json
import argparse
import urllib.request
import urllib.error

QWEN_API_URL = "http://gx10.access.net:8080/coding/v1/chat/completions"
QWEN_MODEL_ID = "unsloth/Qwen3.5-122B-A10B-NVFP4"


def query_qwen(prompt, system_prompt=None, max_tokens=8192, temperature=0.6, include_reasoning=False):
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
        sys.stderr.write(f"Error reaching local Qwen model: {e}\n")
        sys.exit(1)

    final_content = "".join(content_parts).strip()
    reasoning_text = "".join(reasoning_parts).strip()

    if include_reasoning and reasoning_text:
        return f"<thinking>\n{reasoning_text}\n</thinking>\n\n{final_content}"
    elif not final_content and reasoning_text:
        return f"*(Generated reasoning only, reached max_tokens)*\n\n{reasoning_text}"

    return final_content


def main():
    parser = argparse.ArgumentParser(description="Query local Qwen3.5-122B instance")
    parser.add_argument("prompt", nargs="?", help="Prompt to send to the model")
    parser.add_argument("-s", "--system", help="System prompt", default=None)
    parser.add_argument("-m", "--max-tokens", type=int, default=8192, help="Max tokens (default 8192)")
    parser.add_argument("-t", "--temperature", type=float, default=0.6, help="Temperature (default 0.6)")
    parser.add_argument("-r", "--include-reasoning", action="store_true", help="Print reasoning trace")

    args = parser.parse_args()

    prompt = args.prompt
    if not prompt:
        if not sys.stdin.isatty():
            prompt = sys.stdin.read().strip()
        else:
            parser.print_help()
            sys.exit(1)

    if not prompt:
        sys.stderr.write("Error: empty prompt\n")
        sys.exit(1)

    result = query_qwen(
        prompt=prompt,
        system_prompt=args.system,
        max_tokens=args.max_tokens,
        temperature=args.temperature,
        include_reasoning=args.include_reasoning
    )
    print(result)


if __name__ == "__main__":
    main()
