#!/usr/bin/env python3
"""
CLI Client for querying local SearXNG search engine.
Usage:
    python3 searxng_client.py "Jetpack Compose WindowInsets"
    python3 searxng_client.py -n 5 -c it "Kotlin Flow zip vs combine"
"""

import sys
import json
import argparse
import urllib.request
import urllib.parse
import urllib.error

SEARXNG_URL = "http://gx10.access.net:8080/coding/search"


def search(query, categories="it,general", engines=None, max_results=8):
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
        headers={"User-Agent": "AntigravitySearxngClient/1.0"}
    )

    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except urllib.error.URLError as e:
        sys.stderr.write(f"Error querying SearXNG: {e}\n")
        sys.exit(1)

    results = data.get("results", [])
    if not results:
        print(f"No results found for '{query}'")
        return

    for i, item in enumerate(results[:max_results], 1):
        title = item.get("title", "Untitled")
        item_url = item.get("url", "")
        content = item.get("content", "").strip()
        engine = item.get("engine", "")

        engine_badge = f" [{engine}]" if engine else ""
        print(f"{i}. {title}{engine_badge}")
        print(f"   URL: {item_url}")
        if content:
            clean_content = " ".join(content.split())
            print(f"   {clean_content}")
        print()


def main():
    parser = argparse.ArgumentParser(description="Query local SearXNG search instance")
    parser.add_argument("query", nargs="+", help="Search query")
    parser.add_argument("-c", "--categories", default="it,general", help="Categories (default: it,general)")
    parser.add_argument("-e", "--engines", default=None, help="Comma-separated engines (e.g. github,stackoverflow)")
    parser.add_argument("-n", "--num", type=int, default=8, help="Number of results (default: 8)")

    args = parser.parse_args()
    query_str = " ".join(args.query)

    search(
        query=query_str,
        categories=args.categories,
        engines=args.engines,
        max_results=args.num
    )


if __name__ == "__main__":
    main()
