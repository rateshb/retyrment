#!/bin/bash

# Retyrment Application - Server Setup Script
# This script sets up a fresh Ubuntu server with all required dependencies
# Usage: sudo bash setup-server.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    log_error "Please run as root (use sudo)"
    exit 1
fi

# Check if Ubuntu
if [ ! -f /etc/lsb-release ]; then
    log_error "This script is designed for Ubuntu"
    exit 1
fi

log_info "Starting server setup for Retyrment application..."
echo ""

# Update system
log_info "Updating system packages..."
apt update && apt upgrade -y

# Install basic tools
log_info "Installing basic tools..."
apt install -y git curl wget vim htop unzip software-properties-common

# Install Java 17
log_info "Installing Java 17..."
apt install -y openjdk-17-jdk
java -version

# Install Maven
log_info "Installing Maven..."
apt install -y maven
mvn -version

# Install Node.js 20
log_info "Installing Node.js 20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v
npm -v

# Install NGINX
log_info "Installing NGINX..."
apt install -y nginx
systemctl enable nginx
systemctl start nginx

# Install MongoDB
log_info "Installing MongoDB..."
curl -fsSL https://pgp.mongodb.com/server-7.0.asc | \
   gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg \
   --dearmor

echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-7.0.list

apt update
apt install -y mongodb-org
systemctl enable mongod
systemctl start mongod

# Install Certbot for SSL
log_info "Installing Certbot..."
apt install -y certbot python3-certbot-nginx

# Create application directories
log_info "Creating application directories..."
mkdir -p /var/www/retyrment/{react,vanilla}
mkdir -p /var/log/retyrment
mkdir -p /home/ubuntu/backups
mkdir -p /etc/environment.d

# Set permissions
chown -R www-data:www-data /var/www/retyrment
chown -R ubuntu:ubuntu /var/log/retyrment
chown -R ubuntu:ubuntu /home/ubuntu/backups

# Configure MongoDB
log_info "Configuring MongoDB..."
log_warn "MongoDB installed without authentication. Configure authentication in production!"
log_warn "To enable auth, use: sudo nano /etc/mongod.conf and set security.authorization: enabled"

# Configure firewall
log_info "Configuring firewall..."
ufw --force enable
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw status

# Create environment file template
log_info "Creating environment file template..."
cat > /etc/environment.d/retyrment.conf.template <<'EOF'
# Retyrment Application Environment Variables
# Copy this to retyrment.conf and fill in actual values

export MONGODB_URI="mongodb://localhost:27017/retyrment"
# Or for MongoDB Atlas:
# export MONGODB_URI="mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/retyrment?retryWrites=true&w=majority"
export JWT_SECRET="your_jwt_secret_at_least_256_bits_long"
export GOOGLE_CLIENT_ID="your_google_client_id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your_google_client_secret"
EOF

log_warn "Environment template created at /etc/environment.d/retyrment.conf.template"
log_warn "Please copy and configure: sudo cp /etc/environment.d/retyrment.conf.template /etc/environment.d/retyrment.conf"

# Create systemd service for backend
log_info "Creating backend systemd service..."
cat > /etc/systemd/system/retyrment-backend.service <<'EOF'
[Unit]
Description=Retyrment Backend API
After=network.target mongod.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/retyrment/backend
EnvironmentFile=/etc/environment.d/retyrment.conf
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar -Dspring.profiles.active=prod target/retyrment-1.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=retyrment-backend

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload

# Create NGINX configuration template
log_info "Creating NGINX configuration template..."
cat > /etc/nginx/sites-available/retyrment.template <<'EOF'
# Retyrment Application NGINX Configuration
# Replace yourdomain.com with your actual domain

limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;

upstream backend {
    server localhost:8080 fail_timeout=30s max_fails=3;
}

server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com www.yourdomain.com;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://$server_name$request_uri;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    # SSL certificates will be added here by Certbot
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    gzip on;
    gzip_types text/plain text/css text/xml text/javascript application/json application/javascript;
    
    client_max_body_size 10M;
    
    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass http://backend/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /react {
        alias /var/www/retyrment/react;
        try_files $uri $uri/ /react/index.html;
    }
    
    location / {
        root /var/www/retyrment/vanilla;
        try_files $uri $uri/ /index.html;
        index index.html;
    }
    
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
EOF

log_warn "NGINX template created at /etc/nginx/sites-available/retyrment.template"
log_warn "Update domain name and enable: sudo cp /etc/nginx/sites-available/retyrment.template /etc/nginx/sites-available/retyrment"

# Create log rotation configuration
log_info "Setting up log rotation..."
cat > /etc/logrotate.d/retyrment <<'EOF'
/var/log/retyrment/*.log {
    daily
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 ubuntu ubuntu
    sharedscripts
    postrotate
        systemctl reload retyrment-backend > /dev/null 2>&1 || true
    endscript
}
EOF

# Create monitoring script
log_info "Creating monitoring script..."
cat > /home/ubuntu/monitor.sh <<'EOF'
#!/bin/bash

echo "=== Retyrment Application Status ==="
echo ""
echo "Backend: $(systemctl is-active retyrment-backend 2>/dev/null || echo 'not configured')"
echo "NGINX: $(systemctl is-active nginx)"
echo "MongoDB: $(systemctl is-active mongod)"
echo ""
echo "=== System Resources ==="
echo "Disk Usage:"
df -h / | tail -n 1
echo ""
echo "Memory Usage:"
free -h | grep "Mem:"
echo ""
echo "=== Recent Backend Logs ==="
journalctl -u retyrment-backend -n 5 --no-pager 2>/dev/null || echo "Backend not started yet"
EOF

chmod +x /home/ubuntu/monitor.sh
chown ubuntu:ubuntu /home/ubuntu/monitor.sh

# Create backup script
log_info "Creating backup script..."
cat > /home/ubuntu/backup.sh <<'EOF'
#!/bin/bash

BACKUP_DIR="/home/ubuntu/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup MongoDB database
mongodump --uri="${MONGODB_URI:-mongodb://localhost:27017/retyrment}" --out=$BACKUP_DIR/mongo_$DATE 2>/dev/null || echo "Database backup skipped"
tar -czf $BACKUP_DIR/mongo_$DATE.tar.gz -C $BACKUP_DIR mongo_$DATE 2>/dev/null && rm -rf $BACKUP_DIR/mongo_$DATE

# Backup application
if [ -d /home/ubuntu/retyrment ]; then
    tar -czf $BACKUP_DIR/app_$DATE.tar.gz /home/ubuntu/retyrment 2>/dev/null
fi

# Keep only last 7 days
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "Backup completed: $DATE"
EOF

chmod +x /home/ubuntu/backup.sh
chown ubuntu:ubuntu /home/ubuntu/backup.sh

# Print summary
echo ""
echo "=========================================="
log_info "Server setup completed successfully!"
echo "=========================================="
echo ""
log_info "Installed Components:"
echo "  - Java 17: $(java -version 2>&1 | head -n 1)"
echo "  - Maven: $(mvn -version 2>&1 | head -n 1)"
echo "  - Node.js: $(node -v)"
echo "  - NGINX: $(nginx -v 2>&1)"
echo "  - MongoDB: $(mongod --version | head -n 1)"
echo ""
log_warn "Next Steps:"
echo "  1. Configure environment variables:"
echo "     sudo cp /etc/environment.d/retyrment.conf.template /etc/environment.d/retyrment.conf"
echo "     sudo nano /etc/environment.d/retyrment.conf"
echo ""
echo "  2. Clone application repository:"
echo "     cd /home/ubuntu"
echo "     git clone https://github.com/your-username/retyrment.git"
echo ""
echo "  3. Configure NGINX:"
echo "     sudo cp /etc/nginx/sites-available/retyrment.template /etc/nginx/sites-available/retyrment"
echo "     sudo nano /etc/nginx/sites-available/retyrment  # Update domain"
echo "     sudo ln -s /etc/nginx/sites-available/retyrment /etc/nginx/sites-enabled/"
echo "     sudo rm /etc/nginx/sites-enabled/default"
echo "     sudo nginx -t && sudo systemctl reload nginx"
echo ""
echo "  4. Setup SSL certificate:"
echo "     sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com"
echo ""
echo "  5. Build and deploy application:"
echo "     cd /home/ubuntu/retyrment"
echo "     sudo bash scripts/deploy.sh --all"
echo ""
echo "  6. Configure MongoDB authentication (recommended for production):"
echo "     sudo nano /etc/mongod.conf"
echo "     # Add: security.authorization: enabled"
echo ""
log_info "Useful commands:"
echo "  - Monitor status: /home/ubuntu/monitor.sh"
echo "  - Backup: /home/ubuntu/backup.sh"
echo "  - View logs: sudo journalctl -u retyrment-backend -f"
echo ""
