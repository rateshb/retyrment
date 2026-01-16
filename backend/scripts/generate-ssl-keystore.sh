#!/bin/bash
# Script to generate SSL keystore for HTTPS
# Run this script to create a self-signed certificate for development
# For production, use a certificate from Let's Encrypt or other CA

KEYSTORE_PASSWORD=${1:-changeit}
DOMAIN=${2:-localhost}

echo "Generating SSL keystore..."
echo "Domain: $DOMAIN"
echo "Password: $KEYSTORE_PASSWORD"

# Generate keystore
keytool -genkeypair \
    -alias retyrment \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore ../src/main/resources/keystore.p12 \
    -validity 365 \
    -storepass "$KEYSTORE_PASSWORD" \
    -dname "CN=$DOMAIN, OU=Retyrment, O=Retyrment, L=City, ST=State, C=IN"

echo ""
echo "Keystore generated: src/main/resources/keystore.p12"
echo ""
echo "To enable HTTPS, add these to application-prod.yml:"
echo ""
echo "server:"
echo "  ssl:"
echo "    key-store: classpath:keystore.p12"
echo "    key-store-password: $KEYSTORE_PASSWORD"
echo "    key-store-type: PKCS12"
echo "    key-alias: retyrment"
echo ""
echo "For production, use Let's Encrypt instead!"
