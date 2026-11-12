"""Serves this folder on http://localhost:8766 and tells the browser never to cache, so edits show up on a normal reload."""
import http.server
import sys
from pathlib import Path


class Handler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-store, max-age=0")
        super().end_headers()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8766
    root = Path(__file__).resolve().parent
    handler = lambda *a, **k: Handler(*a, directory=str(root), **k)
    http.server.ThreadingHTTPServer(("127.0.0.1", port), handler).serve_forever()
