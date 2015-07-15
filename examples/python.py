# This program requires the requests package (pip install requests)
import requests

# Connect to the NXT Robot
requests.post('http://localhost:8082/connect', "00:16:53:0B:2D:DC")
# Send the "Hello World" string to the bot
requests.post('http://localhost:8082/utf', "Hello World")
# Retrieve the response from the bot
print(requests.get('http://localhost:8082/utf').content)
