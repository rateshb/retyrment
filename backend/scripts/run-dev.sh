#!/bin/bash
# WealthVision - Run Development Server
# Usage: ./run-dev.sh
# Requires: MONGO_URI environment variable

echo "============================================"
echo "  WealthVision - Development Mode"
echo "============================================"
echo ""

cd "$(dirname "$0")/.."

if [ -z "$MONGO_URI" ]; then
    echo "ERROR: MONGO_URI environment variable not set!"
    echo "Set it with: export MONGO_URI=mongodb://your-dev-server:27017/wealthvision_dev"
    exit 1
fi

echo "MongoDB URI: $MONGO_URI"
echo ""

# Build the JAR if not exists
if [ ! -f target/wealthvision-1.0.0.jar ]; then
    echo "Building application..."
    ./mvnw clean package -DskipTests
fi

echo "Starting WealthVision with DEV profile..."
echo ""

java -jar target/wealthvision-1.0.0.jar --spring.profiles.active=dev
