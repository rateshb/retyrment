#!/bin/bash
# WealthVision - Run Production Server
# Usage: ./run-prod.sh
# Requires: MONGO_URI, MONGO_DATABASE environment variables

echo "============================================"
echo "  WealthVision - Production Mode"
echo "============================================"
echo ""

cd "$(dirname "$0")/.."

if [ -z "$MONGO_URI" ]; then
    echo "ERROR: MONGO_URI environment variable not set!"
    exit 1
fi

echo "MongoDB Database: $MONGO_DATABASE"
echo ""

# Build the JAR
echo "Building production JAR..."
./mvnw clean package -DskipTests -Pprod

echo "Starting WealthVision with PROD profile..."
echo ""

java -Xms512m -Xmx1024m \
     -jar target/wealthvision-1.0.0.jar \
     --spring.profiles.active=prod
