#!/bin/sh
# This program requires curl

# Connect to the NXT Robot
curl localhost:8082/connect -X POST -d 00:16:53:0B:2D:DC
# Send the "Hello World" string to the bot
curl localhost:8082/utf/ -X POST -d "Hello World"
# Retrieve the response from the bot
curl localhost:8082/utf/
