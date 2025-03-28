import requests
import time

url = "http://localhost:8080/proxy/api/test"

while True:
    response = requests.get(url)
    print(response.text)
    time.sleep(1)
