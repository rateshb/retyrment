# Deployment Guide - Retyrment Application

Complete guide to deploy the Retyrment application (Spring Boot Backend + React Frontend + VanillaJS Frontend) on AWS EC2 behind NGINX.

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [AWS Setup](#aws-setup)
4. [Server Configuration](#server-configuration)
5. [Backend Deployment](#backend-deployment)
6. [Frontend Deployment](#frontend-deployment)
7. [NGINX Configuration](#nginx-configuration)
8. [SSL/HTTPS Setup](#ssl-https-setup)
9. [Database Configuration](#database-configuration)
10. [Service Management](#service-management)
11. [Monitoring & Maintenance](#monitoring--maintenance)
12. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
Internet
    ↓
AWS EC2 Instance (Public IP)
    ↓
NGINX (Port 80/443)
    ├── /api → Backend (localhost:8080)
    ├── /react → React Frontend (localhost:3000 or static files)
    └── / → VanillaJS Frontend (static files)
```

**Ports:**
- NGINX: 80 (HTTP), 443 (HTTPS)
- Backend: 8080 (internal)
- React Dev Server: 3000 (internal, optional)

---

## Prerequisites

### Local Machine
- AWS Account with EC2 access
- SSH key pair for EC2 access
- Domain name (optional, for HTTPS)

### Server Requirements
- **Instance Type:** t2.medium or better (2 vCPU, 4GB RAM minimum)
- **OS:** Ubuntu 22.04 LTS
- **Storage:** 30GB minimum
- **Database:** MongoDB (can be on same instance, MongoDB Atlas, or AWS DocumentDB)

---

## AWS Setup

### 1. Create EC2 Instance

1. **Launch Instance:**
   ```
   - AMI: Ubuntu Server 22.04 LTS
   - Instance Type: t2.medium
   - Storage: 30GB gp3
   - Key Pair: Create or select existing
   ```

2. **Configure Security Group:**
   ```
   Inbound Rules:
   - SSH (22) - Your IP
   - HTTP (80) - 0.0.0.0/0
   - HTTPS (443) - 0.0.0.0/0
   - Custom (8080) - Only from NGINX (optional for testing)
   
   Outbound Rules:
   - All traffic - 0.0.0.0/0
   ```

3. **Allocate Elastic IP (recommended):**
   - Navigate to EC2 → Elastic IPs
   - Allocate new address
   - Associate with your instance

### 2. Connect to Instance

```bash
chmod 400 your-key.pem
ssh -i your-key.pem ubuntu@<your-elastic-ip>
```

---

## Server Configuration

### 1. Update System

```bash
sudo apt update && sudo apt upgrade -y
```

### 2. Install Java (for Backend)

```bash
# Install OpenJDK 17
sudo apt install openjdk-17-jdk -y

# Verify
java -version
```

### 3. Install Maven

```bash
sudo apt install maven -y
mvn -version
```

### 4. Install Node.js (for React Frontend)

```bash
# Install Node.js 20.x
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Verify
node -v
npm -v
```

### 5. Install NGINX

```bash
sudo apt install nginx -y
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 6. Install Git

```bash
sudo apt install git -y
```

### 7. Install MongoDB (if using local DB)

```bash
# Import MongoDB public GPG key
curl -fsSL https://pgp.mongodb.com/server-7.0.asc | \
   sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg \
   --dearmor

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Update and install MongoDB
sudo apt update
sudo apt install -y mongodb-org

# Enable and start MongoDB
sudo systemctl enable mongod
sudo systemctl start mongod
sudo systemctl status mongod
```

---

## Backend Deployment

### 1. Clone Repository

```bash
cd /home/ubuntu
git clone https://github.com/your-username/retyrment.git
cd retyrment/backend
```

### 2. Configure Application Properties

```bash
# Create production configuration
sudo nano src/main/resources/application-prod.yml
```

**application-prod.yml:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/retyrment}
      # For MongoDB Atlas:
      # uri: mongodb+srv://${MONGODB_USER}:${MONGODB_PASSWORD}@cluster0.xxxxx.mongodb.net/retyrment?retryWrites=true&w=majority
  
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: https://retyrment.com/api/oauth2/callback
            scope:
              - openid
              - profile
              - email

app:
  frontend-url: https://retyrment.com
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000
  cors:
    allowed-origins: https://retyrment.com,https://www.retyrment.com
  defaults:
    inflation-rate: 6.0
    epf-return: 8.15
    ppf-return: 7.1
    mf-return: 12.0
    nps-return: 10.0

logging:
  level:
    root: INFO
    com.retyrment: INFO
  file:
    name: /var/log/retyrment/application.log
```

### 3. Set Environment Variables

```bash
# Create environment file
sudo nano /etc/environment.d/retyrment.conf
```

**retyrment.conf:**
```bash
export MONGODB_URI="mongodb://localhost:27017/retyrment"
# Or for MongoDB Atlas:
# export MONGODB_URI="mongodb+srv://retyrment_user:your_password@cluster0.xxxxx.mongodb.net/retyrment?retryWrites=true&w=majority"
export JWT_SECRET="your_jwt_secret_at_least_256_bits"
export GOOGLE_CLIENT_ID="your_google_client_id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your_google_client_secret"
```

```bash
# Load environment variables
source /etc/environment.d/retyrment.conf
```

### 4. Build Backend

```bash
cd /home/ubuntu/retyrment/backend
mvn clean package -DskipTests -Pprod
```

### 5. Create Systemd Service

```bash
sudo nano /etc/systemd/system/retyrment-backend.service
```

**retyrment-backend.service:**
```ini
[Unit]
Description=Retyrment Backend API
After=network.target mongod.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/retyrment/backend
EnvironmentFile=/etc/environment.d/retyrment.conf
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod target/retyrment-1.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=retyrment-backend

[Install]
WantedBy=multi-user.target
```

### 6. Start Backend Service

```bash
sudo systemctl daemon-reload
sudo systemctl enable retyrment-backend
sudo systemctl start retyrment-backend

# Check status
sudo systemctl status retyrment-backend

# View logs
sudo journalctl -u retyrment-backend -f
```

---

## Frontend Deployment

### Option A: Production Build (Recommended)

#### React Frontend

```bash
cd /home/ubuntu/retyrment/frontend-react

# Create production environment file
nano .env.production
```

**.env.production:**
```
VITE_API_URL=https://retyrment.com/api
```

```bash
# Build for production
npm install
npm run build

# Copy build to NGINX directory
sudo mkdir -p /var/www/retyrment/react
sudo cp -r dist/* /var/www/retyrment/react/
sudo chown -R www-data:www-data /var/www/retyrment/react
```

#### VanillaJS Frontend

```bash
cd /home/ubuntu/retyrment/frontend

# Update API configuration
nano js/api.js
```

**Update API_BASE in js/api.js:**
```javascript
const API_BASE = 'https://retyrment.com/api';
```

```bash
# Copy to NGINX directory
sudo mkdir -p /var/www/retyrment/vanilla
sudo cp -r * /var/www/retyrment/vanilla/
sudo chown -R www-data:www-data /var/www/retyrment/vanilla
```

### Option B: React Dev Server (Not Recommended for Production)

```bash
cd /home/ubuntu/retyrment/frontend-react

# Create systemd service
sudo nano /etc/systemd/system/retyrment-react.service
```

**retyrment-react.service:**
```ini
[Unit]
Description=Retyrment React Frontend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/retyrment/frontend-react
Environment="PORT=3000"
ExecStart=/usr/bin/npm run dev -- --host
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable retyrment-react
sudo systemctl start retyrment-react
```

---

## NGINX Configuration

### 1. Create NGINX Configuration

```bash
sudo nano /etc/nginx/sites-available/retyrment
```

### Option A: Production Build Configuration (Recommended)

**retyrment (production build):**
```nginx
# Rate limiting
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;

# Backend upstream
upstream backend {
    server localhost:8080 fail_timeout=30s max_fails=3;
}

# HTTP Server (redirect to HTTPS)
server {
    listen 80;
    listen [::]:80;
    server_name retyrment.com www.retyrment.com;
    
    # Let's Encrypt validation
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    # Redirect all HTTP to HTTPS
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS Server
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name retyrment.com www.retyrment.com;
    
    # SSL Configuration (will be added by Certbot)
    # ssl_certificate /etc/letsencrypt/live/retyrment.com/fullchain.pem;
    # ssl_certificate_key /etc/letsencrypt/live/retyrment.com/privkey.pem;
    
    # SSL Security Settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self' https: data: 'unsafe-inline' 'unsafe-eval';" always;
    
    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/json application/javascript;
    
    # Max upload size
    client_max_body_size 10M;
    
    # Backend API
    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;
        
        proxy_pass http://backend/api/;
        proxy_http_version 1.1;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # OAuth callback specific
        location /api/oauth2/ {
            limit_req zone=login_limit burst=5 nodelay;
            proxy_pass http://backend/api/oauth2/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
    
    # React Frontend (served at /react)
    location /react {
        alias /var/www/retyrment/react;
        try_files $uri $uri/ /react/index.html;
        
        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)$ {
            alias /var/www/retyrment/react;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
    
    # VanillaJS Frontend (default)
    location / {
        root /var/www/retyrment/vanilla;
        try_files $uri $uri/ /index.html;
        index index.html;
        
        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)$ {
            root /var/www/retyrment/vanilla;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
    
    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

### Option B: React Dev Server Configuration

**retyrment (with React dev server):**
```nginx
# Add this upstream for React dev server
upstream react_frontend {
    server localhost:3000;
}

# Then in the server block, replace React location with:
location /react {
    proxy_pass http://react_frontend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
    proxy_set_header Host $host;
    proxy_cache_bypass $http_upgrade;
}
```

### 2. Enable Configuration

```bash
# Test configuration
sudo nginx -t

# Enable site
sudo ln -s /etc/nginx/sites-available/retyrment /etc/nginx/sites-enabled/

# Remove default site
sudo rm /etc/nginx/sites-enabled/default

# Reload NGINX
sudo systemctl reload nginx
```

---

## SSL/HTTPS Setup

### Using Let's Encrypt (Certbot)

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtain certificate
sudo certbot --nginx -d retyrment.com -d www.retyrment.com

# Certbot will automatically update your NGINX configuration
# Test automatic renewal
sudo certbot renew --dry-run

# Auto-renewal is set up via systemd timer
sudo systemctl status certbot.timer
```

### Manual SSL Certificate

If using a purchased SSL certificate:

```bash
# Copy certificate files
sudo mkdir -p /etc/ssl/certs/retyrment
sudo cp fullchain.pem /etc/ssl/certs/retyrment/
sudo cp privkey.pem /etc/ssl/certs/retyrment/

# Update NGINX configuration
sudo nano /etc/nginx/sites-available/retyrment
```

Add in the HTTPS server block:
```nginx
ssl_certificate /etc/ssl/certs/retyrment/fullchain.pem;
ssl_certificate_key /etc/ssl/certs/retyrment/privkey.pem;
```

---

## Database Configuration

### MongoDB Setup (Local)

```bash
# MongoDB is already running from installation
# Create database and user
mongosh

# Switch to admin database
use admin

# Create user
db.createUser({
  user: "retyrment_user",
  pwd: "your_secure_password",
  roles: [
    { role: "readWrite", db: "retyrment" }
  ]
})

# Switch to retyrment database
use retyrment

# Verify connection
db.runCommand({ ping: 1 })

# Exit
exit
```

### Enable Authentication (Recommended for Production)

```bash
# Edit MongoDB configuration
sudo nano /etc/mongod.conf
```

Update:
```yaml
security:
  authorization: enabled
```

```bash
# Restart MongoDB
sudo systemctl restart mongod
```

Update environment variable:
```bash
export MONGODB_URI="mongodb://retyrment_user:your_secure_password@localhost:27017/retyrment?authSource=admin"
```

### Using MongoDB Atlas (Recommended for Production)

1. Create account at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Create a cluster (Free tier available)
3. Create database user
4. Whitelist your EC2 instance IP
5. Get connection string
6. Update `application-prod.yml`:
   ```yaml
   spring:
     data:
       mongodb:
         uri: mongodb+srv://retyrment_user:password@cluster0.xxxxx.mongodb.net/retyrment?retryWrites=true&w=majority
   ```

### Using AWS DocumentDB (Alternative)

1. Create DocumentDB cluster in AWS Console
2. Note the endpoint URL
3. Download SSL certificate
4. Update `application-prod.yml`:
   ```yaml
   spring:
     data:
       mongodb:
         uri: mongodb://username:password@docdb-cluster.cluster-xxxxx.region.docdb.amazonaws.com:27017/retyrment?ssl=true&replicaSet=rs0&readPreference=secondaryPreferred
   ```

---

## Service Management

### Start All Services

```bash
# Backend
sudo systemctl start retyrment-backend

# React (if using dev server)
sudo systemctl start retyrment-react

# NGINX
sudo systemctl start nginx
```

### Check Service Status

```bash
# Backend
sudo systemctl status retyrment-backend
sudo journalctl -u retyrment-backend -f

# React
sudo systemctl status retyrment-react
sudo journalctl -u retyrment-react -f

# NGINX
sudo systemctl status nginx
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log
```

### Restart Services

```bash
sudo systemctl restart retyrment-backend
sudo systemctl restart retyrment-react
sudo systemctl reload nginx
```

### Enable Services on Boot

```bash
sudo systemctl enable retyrment-backend
sudo systemctl enable retyrment-react
sudo systemctl enable nginx
```

---

## Monitoring & Maintenance

### 1. Log Management

```bash
# Create log directories
sudo mkdir -p /var/log/retyrment
sudo chown ubuntu:ubuntu /var/log/retyrment

# Configure log rotation
sudo nano /etc/logrotate.d/retyrment
```

**/etc/logrotate.d/retyrment:**
```
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

/var/log/nginx/*.log {
    daily
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        systemctl reload nginx > /dev/null 2>&1 || true
    endscript
}
```

### 2. Monitoring Script

Create monitoring script:

```bash
nano ~/monitor.sh
```

**monitor.sh:**
```bash
#!/bin/bash

echo "=== Retyrment Application Status ==="
echo ""
echo "Backend Service:"
systemctl is-active retyrment-backend
echo ""
echo "NGINX Service:"
systemctl is-active nginx
echo ""
echo "Disk Usage:"
df -h | grep -E '^/dev/'
echo ""
echo "Memory Usage:"
free -h
echo ""
echo "Backend Logs (last 5 lines):"
journalctl -u retyrment-backend -n 5 --no-pager
```

```bash
chmod +x ~/monitor.sh
```

### 3. Backup Script

```bash
nano ~/backup.sh
```

**backup.sh:**
```bash
#!/bin/bash

BACKUP_DIR="/home/ubuntu/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup MongoDB database
mongodump --uri="${MONGODB_URI}" --out=$BACKUP_DIR/mongo_$DATE
# Compress the backup
tar -czf $BACKUP_DIR/mongo_$DATE.tar.gz -C $BACKUP_DIR mongo_$DATE
rm -rf $BACKUP_DIR/mongo_$DATE

# Backup application files
tar -czf $BACKUP_DIR/app_$DATE.tar.gz /home/ubuntu/retyrment

# Keep only last 7 days of backups
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "Backup completed: $DATE"
```

```bash
chmod +x ~/backup.sh

# Add to crontab for daily backups at 2 AM
crontab -e
```

Add:
```
0 2 * * * /home/ubuntu/backup.sh >> /home/ubuntu/backup.log 2>&1
```

### 4. System Updates

```bash
# Weekly updates (add to crontab)
0 3 * * 0 sudo apt update && sudo apt upgrade -y && sudo systemctl restart retyrment-backend
```

---

## Troubleshooting

### Backend Not Starting

```bash
# Check logs
sudo journalctl -u retyrment-backend -n 100 --no-pager

# Common issues:
# 1. Port already in use
sudo lsof -i :8080

# 2. Database connection
mongosh $MONGODB_URI

# 3. Permission issues
ls -la /home/ubuntu/retyrment/backend/target/
```

### NGINX 502 Bad Gateway

```bash
# Check if backend is running
sudo systemctl status retyrment-backend

# Check NGINX error logs
sudo tail -f /var/log/nginx/error.log

# Test backend directly
curl http://localhost:8080/api/health

# Check NGINX configuration
sudo nginx -t
```

### CORS Errors

Update `application-prod.yml`:
```yaml
app:
  cors:
    allowed-origins: https://retyrment.com,https://www.retyrment.com,http://localhost:3000
```

### SSL Certificate Issues

```bash
# Check certificate expiry
sudo certbot certificates

# Renew manually
sudo certbot renew

# Check NGINX SSL configuration
sudo nginx -t
```

### High Memory Usage

```bash
# Check Java heap size
ps aux | grep java

# Adjust in systemd service
sudo nano /etc/systemd/system/retyrment-backend.service
```

Modify ExecStart:
```ini
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar -Dspring.profiles.active=prod target/retyrment-1.0.0.jar
```

### Database Connection Pool Exhausted

Update `application-prod.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## Deployment Checklist

- [ ] AWS EC2 instance created and accessible
- [ ] Security groups configured
- [ ] Elastic IP allocated
- [ ] Java, Maven, Node.js installed
- [ ] NGINX installed and running
- [ ] PostgreSQL database created
- [ ] Backend compiled and service created
- [ ] Frontend(s) built and deployed
- [ ] NGINX configuration created and tested
- [ ] SSL certificate installed
- [ ] Environment variables set
- [ ] All services started and enabled
- [ ] Log rotation configured
- [ ] Backup script scheduled
- [ ] Monitoring script created
- [ ] DNS records updated (if using domain)
- [ ] Application tested in production
- [ ] Google OAuth redirect URIs updated
- [ ] Firewall rules verified

---

## Post-Deployment

### Update OAuth Redirect URIs

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Navigate to APIs & Services → Credentials
4. Edit your OAuth 2.0 Client ID
5. Add Authorized redirect URIs:
   - `https://retyrment.com/api/oauth2/callback`
   - `https://www.retyrment.com/api/oauth2/callback`

### Test Application

1. **Health Check:**
   ```bash
   curl https://retyrment.com/health
   curl https://retyrment.com/api/health
   ```

2. **Frontend Access:**
   - VanillaJS: https://retyrment.com
   - React: https://retyrment.com/react

3. **API Test:**
   ```bash
   curl https://retyrment.com/api/health
   ```

4. **Login Flow:**
   - Test Google OAuth login
   - Verify redirect works correctly

### Performance Optimization

1. **Enable Caching:**
   ```nginx
   proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=100m;
   
   location /api/ {
       proxy_cache api_cache;
       proxy_cache_valid 200 5m;
       add_header X-Cache-Status $upstream_cache_status;
   }
   ```

2. **Enable HTTP/2:**
   Already enabled in configuration with `http2` flag

3. **CDN Integration:**
   Consider using AWS CloudFront for static assets

---

## Support & Resources

- **Application Logs:** `/var/log/retyrment/application.log`
- **NGINX Logs:** `/var/log/nginx/access.log`, `/var/log/nginx/error.log`
- **Service Status:** `sudo systemctl status retyrment-backend`
- **Database Logs:** `/var/log/postgresql/postgresql-14-main.log`

For issues, check the logs first, then refer to the Troubleshooting section.
