#!/usr/bin/env python3
import requests
import time
import random
import threading
import json

# Configuration
BASE_URL = "http://localhost:8080" 
AUTH_LOGIN_PATH = "/auth/auth/login"

REQUEST_DELAY = 1.0  # seconds between requests
REQUEST_TIMEOUT = 5.0  # seconds before giving up on a request

# Service prefixes should match the keys in ServiceNameResolver.java
SERVICES_ENDPOINTS = {
    "features": ["/", "/list", "/detail/1"],
    "toggles": ["/", "/list", "/detail/1"],
    "analytics": ["/stats", "/report", "/"]
}
UNAUTHENTICATED_PATHS = [
    "/actuator/health",
    "/auth/actuator/health",
    "/features/actuator/health",
    "/toggles/actuator/health",
    "/analytics/actuator/health"
]

# --- JWT Authentication --- #
JWT_TOKEN = None

def login_and_get_token(username="user", password="password"):
    global JWT_TOKEN
    login_url = f"{BASE_URL}{AUTH_LOGIN_PATH}"
    credentials = {"username": username, "password": password}
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    try:
        print(f"{timestamp}  Attempting login to {login_url} as {username}...")
        resp = requests.post(login_url, json=credentials, timeout=REQUEST_TIMEOUT)
        if resp.status_code == 200:
            JWT_TOKEN = resp.json().get("token")
            if JWT_TOKEN:
                print(f"{timestamp}  Login successful. Token obtained.")
            else:
                print(f"{timestamp}  Login successful, but no token in response: {resp.text}")
        else:
            print(f"{timestamp}  Login failed for {login_url}: {resp.status_code} {resp.text}")
    except requests.exceptions.RequestException as e:
        print(f"{timestamp}  Login request error for {login_url}: {e.__class__.__name__} {e}")
    return JWT_TOKEN

# --- Request Simulation --- #
def simulate_request(path: str, use_auth: bool = True):
    url = f"{BASE_URL}{path}"
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    headers = {}
    if use_auth and JWT_TOKEN:
        headers["Authorization"] = f"Bearer {JWT_TOKEN}"
    elif use_auth and not JWT_TOKEN:
        print(f"{timestamp}  Skipping authenticated request to {path:40} - No JWT_TOKEN available.")
        return

    try:
        resp = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
        print(f"{timestamp}  {path:40} (Auth: {use_auth}) → {resp.status_code}  {resp.text[:100]}")
    except requests.exceptions.RequestException as e:
        print(f"{timestamp}  {path:40} (Auth: {use_auth}) → ERROR: {e.__class__.__name__} {e}")

def worker_loop():
    while True:
        if random.random() < 0.2 and UNAUTHENTICATED_PATHS: # 20% chance for unauthenticated
            path = random.choice(UNAUTHENTICATED_PATHS)
            simulate_request(path, use_auth=False)
        elif JWT_TOKEN: 
            service_prefix = random.choice(list(SERVICES_ENDPOINTS.keys())) 
            
            endpoint_sub_path = random.choice(SERVICES_ENDPOINTS[service_prefix])
            
            path = f"/{service_prefix}{endpoint_sub_path}"
            simulate_request(path, use_auth=True)
        else:
            print(f"{time.strftime('%Y-%m-%d %H:%M:%S')}  Waiting for JWT_TOKEN or unauth paths...")
        
        time.sleep(REQUEST_DELAY)

if __name__ == "__main__":
    print("--- Starting Requests Simulation ---")
    
    if not login_and_get_token():
        print("Failed to obtain JWT token. Authenticated requests will be skipped.")

    THREAD_COUNT = 2
    threads = []
    print(f"\nStarting {THREAD_COUNT} simulation worker thread(s)...")
    for i in range(THREAD_COUNT):
        t = threading.Thread(target=worker_loop, name=f"Worker-{i+1}", daemon=True)
        t.start()
        threads.append(t)

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n--- Stopping simulation. ---")
