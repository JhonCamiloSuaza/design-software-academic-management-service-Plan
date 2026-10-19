"""Temporary training-center response for the local transactional-outbox demo."""

import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

CENTER_ID = "a0000000-0000-0000-0000-000000000005"
PATH = f"/api/v1/training-centers/{CENTER_ID}"


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != PATH:
            self.send_error(404)
            return

        body = json.dumps({"id": CENTER_ID, "is_active": True}).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        pass


ThreadingHTTPServer(("127.0.0.1", 8002), Handler).serve_forever()
