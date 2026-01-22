#!/bin/bash

# Retyrment Application Deployment Script
# Usage: ./deploy.sh [--backend|--frontend|--all]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 is not installed. Please install it first."
        exit 1
    fi
}

# Check if running on Ubuntu
if [ ! -f /etc/lsb-release ]; then
    log_error "This script is designed for Ubuntu. Please deploy manually."
    exit 1
fi

# Configuration
APP_DIR="/home/ubuntu/retyrment"
NGINX_DIR="/var/www/retyrment"
LOG_DIR="/var/log/retyrment"

# Parse arguments
DEPLOY_BACKEND=false
DEPLOY_FRONTEND=false

if [ "$1" == "--backend" ]; then
    DEPLOY_BACKEND=true
elif [ "$1" == "--frontend" ]; then
    DEPLOY_FRONTEND=true
elif [ "$1" == "--all" ] || [ -z "$1" ]; then
    DEPLOY_BACKEND=true
    DEPLOY_FRONTEND=true
else
    log_error "Invalid argument. Use --backend, --frontend, or --all"
    exit 1
fi

log_info "Starting deployment..."

# Backend Deployment
if [ "$DEPLOY_BACKEND" = true ]; then
    log_info "Deploying backend..."
    
    # Check required commands
    check_command java
    check_command mvn
    
    # Navigate to backend directory
    cd $APP_DIR/backend
    
    # Pull latest changes
    log_info "Pulling latest changes..."
    git pull origin main
    
    # Build application
    log_info "Building backend..."
    mvn clean package -DskipTests -Pprod
    
    # Restart service
    log_info "Restarting backend service..."
    sudo systemctl restart retyrment-backend
    
    # Wait for service to start
    sleep 5
    
    # Check service status
    if sudo systemctl is-active --quiet retyrment-backend; then
        log_info "Backend deployed successfully!"
    else
        log_error "Backend deployment failed. Check logs with: sudo journalctl -u retyrment-backend -n 50"
        exit 1
    fi
fi

# Frontend Deployment
if [ "$DEPLOY_FRONTEND" = true ]; then
    log_info "Deploying frontends..."
    
    # Check required commands
    check_command node
    check_command npm
    
    # Deploy React Frontend
    log_info "Building React frontend..."
    cd $APP_DIR/frontend-react
    
    # Pull latest changes
    git pull origin main
    
    # Install dependencies and build
    npm install
    npm run build
    
    # Copy to NGINX directory
    log_info "Copying React build to NGINX..."
    sudo mkdir -p $NGINX_DIR/react
    sudo rm -rf $NGINX_DIR/react/*
    sudo cp -r dist/* $NGINX_DIR/react/
    sudo chown -R www-data:www-data $NGINX_DIR/react
    
    log_info "React frontend deployed successfully!"
    
    # Deploy VanillaJS Frontend
    log_info "Deploying VanillaJS frontend..."
    cd $APP_DIR/frontend
    
    # Pull latest changes
    git pull origin main
    
    # Copy to NGINX directory
    sudo mkdir -p $NGINX_DIR/vanilla
    sudo rm -rf $NGINX_DIR/vanilla/*
    sudo cp -r * $NGINX_DIR/vanilla/
    sudo chown -R www-data:www-data $NGINX_DIR/vanilla
    
    log_info "VanillaJS frontend deployed successfully!"
fi

# Reload NGINX
log_info "Reloading NGINX..."
sudo nginx -t && sudo systemctl reload nginx

# Display status
echo ""
log_info "Deployment Summary:"
echo "===================="

if [ "$DEPLOY_BACKEND" = true ]; then
    echo "Backend Status: $(sudo systemctl is-active retyrment-backend)"
fi

if [ "$DEPLOY_FRONTEND" = true ]; then
    echo "Frontends: Deployed"
fi

echo "NGINX Status: $(sudo systemctl is-active nginx)"
echo ""
log_info "Deployment completed successfully!"
log_info "Access your application at: https://$(curl -s ifconfig.me)"
