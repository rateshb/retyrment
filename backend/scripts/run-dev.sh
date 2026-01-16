#!/bin/bash
# Retyrment - Run Development Server
# Usage: ./run-dev.sh
# Requires: MONGO_URI environment variable

echo "============================================"
echo "  Retyrment - Development Mode"
echo "============================================"
echo ""

cd "$(dirname "$0")/.."

if [ -z "$MONGO_URI" ]; then
    echo "ERROR: MONGO_URI environment variable not set!"
    echo "Set it with: export MONGO_URI=mongodb://your-dev-server:27017/retyrment_dev"
    exit 1
fi

echo "MongoDB URI: $MONGO_URI"
echo ""

# Build the JAR if not exists
if [ ! -f target/retyrment-1.0.0.jar ]; then
    echo "Building application..."
    ./mvnw clean package -DskipTests
fi

echo "Starting Retyrment with DEV profile..."
echo ""

java -jar target/retyrment-1.0.0.jar --spring.profiles.active=dev
