#!/bin/bash
# Kills whatever process is holding port 8080
PID=$(lsof -ti:8080)
if [ -z "$PID" ]; then
  echo "Port 8080 is free."
else
  echo "Killing process $PID on port 8080..."
  kill -9 $PID
  echo "Done."
fi
