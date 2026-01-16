#!/bin/bash
# WealthVision - Run Local Development Server
# Usage: ./run-local.sh

echo "============================================"
echo "  WealthVision - Local Development Mode"
echo "============================================"
echo ""

cd "$(dirname "$0")/.."

# Check if MongoDB is running
if ! pgrep -x "mongod" > /dev/null; then
    echo "WARNING: MongoDB doesn't seem to be running!"
    echo "Start MongoDB with: mongod --dbpath /path/to/data"
    echo ""
fi

echo "Starting WealthVision with LOCAL profile..."
echo ""

./mvnw spring-boot:run -Dspring-boot.run.profiles=local
