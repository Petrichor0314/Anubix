#!/usr/bin/env python3
import requests
import time
import random
import threading

# ► Configuration
BASE_URL = "http://localhost:8080"
REQUEST_DELAY = 1.0           # seconds between requests
REQUEST_TIMEOUT = 3.0         # seconds before giving up on a request
SERVICES = ["features", "toggles", "analytics", "unknown"]
ENDPOINTS = {
    "features":  ["/", "/list"],
    "toggles":   ["/", "/status"],
    "analytics": ["/", "/stats"],
    "unknown":   ["/", "/loo", "/laa"]
}

def simulate_request(service: str, subpath: str):
    path = f"/{service}{subpath}"
    url = f"{BASE_URL}{path}"
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    try:
        resp = requests.get(url, timeout=REQUEST_TIMEOUT)
        print(f"{timestamp}  {path:20} → {resp.status_code}  {resp.text}")
    except requests.exceptions.RequestException as e:
        print(f"{timestamp}  {path:20} → ERROR: {e.__class__.__name__} {e}")

def worker_loop():
    while True:
        svc = random.choice(SERVICES)
        sub = random.choice(ENDPOINTS[svc])
        simulate_request(svc, sub)
        time.sleep(REQUEST_DELAY)

if __name__ == "__main__":
    # If you want N concurrent clients, spin up threads here:
    THREAD_COUNT = 1  # increase to simulate more load
    threads = []
    for _ in range(THREAD_COUNT):
        t = threading.Thread(target=worker_loop, daemon=True)
        t.start()
        threads.append(t)

    # Keep the main thread alive:
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping simulation.")
