# Retyrment - Production Deployment Guide

This guide covers deploying Retyrment on a single AWS EC2 instance with NGINX as the web server.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS EC2 Instance                        │
│                                                                 │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐     │
│  │    NGINX    │──────│   Backend   │──────│   MongoDB   │     │
│  │   (Port 80) │      │ (Port 8080) │      │ (Port 27017)│     │
│  │  (Port 443) │      │  Spring Boot │      │             │     │
│  └─────────────┘      └─────────────┘      └─────────────┘     │
│         │                                                       │
│         │ Serves static frontend files                          │
│         │ Proxies /api/* to backend                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### 1. AWS Account Setup
- AWS account with EC2 access
- Security group configured
- Elastic IP (optional but recommended)
- Domain name (e.g., retyrment.com)

### 2. Required Software Versions
- Java 17+
- MongoDB 5.0+
- NGINX 1.18+
- Node.js 18+ (optional, for building)

---

## Step-by-Step Deployment

### Step 1: Launch EC2 Instance

1. **Launch Instance**
   - AMI: Ubuntu 22.04 LTS
   - Instance Type: t3.medium (2 vCPU, 4GB RAM) minimum
   - Storage: 30GB SSD minimum
   
2. **Configure Security Group**
   ```
   Inbound Rules:
   ┌──────────┬──────────┬─────────────────┐
   │ Type     │ Port     │ Source          │
   ├──────────┼──────────┼─────────────────┤
   │ SSH      │ 22       │ Your IP         │
   │ HTTP     │ 80       │ 0.0.0.0/0       │
   │ HTTPS    │ 443      │ 0.0.0.0/0       │
   └──────────┴──────────┴─────────────────┘
   ```

3. **Connect to Instance**
   ```bash
   ssh -i your-key.pem ubuntu@your-ec2-public-ip
   ```

---

### Step 2: Install System Dependencies

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 17
sudo apt install -y openjdk-17-jdk

# Verify Java
java -version

# Install NGINX
sudo apt install -y nginx

# Install certbot for SSL
sudo apt install -y certbot python3-certbot-nginx

# Install MongoDB
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | \
   sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | \
   sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

sudo apt update
sudo apt install -y mongodb-org

# Start and enable MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod

# Verify MongoDB
sudo systemctl status mongod
```

---

### Step 3: Configure MongoDB Security

```bash
# Connect to MongoDB
mongosh

# Create admin user
use admin
db.createUser({
  user: "retyrment_admin",
  pwd: "YOUR_SECURE_PASSWORD_HERE",
  roles: [{ role: "userAdminAnyDatabase", db: "admin" }]
})

# Create application database and user
use retyrment_prod
db.createUser({
  user: "retyrment_app",
  pwd: "YOUR_APP_PASSWORD_HERE",
  roles: [{ role: "readWrite", db: "retyrment_prod" }]
})

exit
```

**Enable MongoDB Authentication:**
```bash
sudo nano /etc/mongod.conf
```

Add/modify:
```yaml
security:
  authorization: enabled

net:
  port: 27017
  bindIp: 127.0.0.1
```

Restart MongoDB:
```bash
sudo systemctl restart mongod
```

---

### Step 4: Deploy Backend Application

1. **Create application directory**
   ```bash
   sudo mkdir -p /opt/retyrment
   sudo chown ubuntu:ubuntu /opt/retyrment
   cd /opt/retyrment
   ```

2. **Upload JAR file** (from your local machine)
   ```bash
   # On your local machine, build the JAR
   cd backend
   mvn clean package -DskipTests -Pprod
   
   # Upload to server
   scp -i your-key.pem target/retyrment-1.0.0.jar ubuntu@your-ec2-ip:/opt/retyrment/
   ```

3. **Create environment file**
   ```bash
   sudo nano /opt/retyrment/.env
   ```
   
   Add:
   ```bash
   # MongoDB
   MONGO_URI=mongodb://retyrment_app:YOUR_APP_PASSWORD@localhost:27017/retyrment_prod?authSource=retyrment_prod
   MONGO_DATABASE=retyrment_prod
   
   # JWT Secret (generate a secure random string)
   JWT_SECRET=your-super-secure-jwt-secret-key-at-least-256-bits-long-change-this
   
   # Google OAuth2 (get from Google Cloud Console)
   GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your-google-client-secret
   
   # Application URLs
   APP_URL=https://retyrment.com
   FRONTEND_URL=https://retyrment.com
   
   # Admin emails
   ADMIN_EMAILS=your-admin@email.com
   
   # CORS
   CORS_ORIGINS=https://retyrment.com
   ```

4. **Create systemd service**
   ```bash
   sudo nano /etc/systemd/system/retyrment.service
   ```
   
   Add:
   ```ini
   [Unit]
   Description=Retyrment Backend Service
   After=network.target mongod.service
   
   [Service]
   Type=simple
   User=ubuntu
   WorkingDirectory=/opt/retyrment
   EnvironmentFile=/opt/retyrment/.env
   ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/retyrment/retyrment-1.0.0.jar --spring.profiles.active=prod
   Restart=always
   RestartSec=10
   StandardOutput=append:/var/log/retyrment/app.log
   StandardError=append:/var/log/retyrment/error.log
   
   [Install]
   WantedBy=multi-user.target
   ```

5. **Create log directory and start service**
   ```bash
   sudo mkdir -p /var/log/retyrment
   sudo chown ubuntu:ubuntu /var/log/retyrment
   
   sudo systemctl daemon-reload
   sudo systemctl enable retyrment
   sudo systemctl start retyrment
   
   # Check status
   sudo systemctl status retyrment
   
   # View logs
   tail -f /var/log/retyrment/app.log
   ```

---

### Step 5: Deploy Frontend

1. **Create frontend directory**
   ```bash
   sudo mkdir -p /var/www/retyrment
   sudo chown ubuntu:ubuntu /var/www/retyrment
   ```

2. **Upload frontend files** (from your local machine)
   ```bash
   # On your local machine
   scp -i your-key.pem -r frontend/* ubuntu@your-ec2-ip:/var/www/retyrment/
   ```

3. **Update API base URL in frontend**
   ```bash
   # On server, edit the API configuration
   nano /var/www/retyrment/js/api.js
   ```
   
   Update `API_BASE_URL`:
   ```javascript
   const API_BASE_URL = 'https://retyrment.com/api';
   ```

---

### Step 6: Configure NGINX

1. **Create NGINX configuration**
   ```bash
   sudo nano /etc/nginx/sites-available/retyrment
   ```
   
   Add:
   ```nginx
   server {
       listen 80;
       server_name retyrment.com www.retyrment.com;
       
       # Redirect HTTP to HTTPS
       return 301 https://$server_name$request_uri;
   }
   
   server {
       listen 443 ssl http2;
       server_name retyrment.com www.retyrment.com;
       
       # SSL certificates (will be added by certbot)
       # ssl_certificate /etc/letsencrypt/live/retyrment.com/fullchain.pem;
       # ssl_certificate_key /etc/letsencrypt/live/retyrment.com/privkey.pem;
       
       # Frontend static files
       root /var/www/retyrment;
       index index.html;
       
       # Security headers
       add_header X-Frame-Options "SAMEORIGIN" always;
       add_header X-XSS-Protection "1; mode=block" always;
       add_header X-Content-Type-Options "nosniff" always;
       add_header Referrer-Policy "strict-origin-when-cross-origin" always;
       
       # Gzip compression
       gzip on;
       gzip_vary on;
       gzip_min_length 1024;
       gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
       
       # API proxy to backend
       location /api/ {
           proxy_pass http://127.0.0.1:8080/api/;
           proxy_http_version 1.1;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_connect_timeout 60s;
           proxy_send_timeout 60s;
           proxy_read_timeout 60s;
           
           # CORS headers (if needed)
           add_header 'Access-Control-Allow-Origin' '$http_origin' always;
           add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
           add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
           add_header 'Access-Control-Allow-Credentials' 'true' always;
           
           if ($request_method = 'OPTIONS') {
               return 204;
           }
       }
       
       # Static file caching
       location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
           expires 30d;
           add_header Cache-Control "public, immutable";
       }
       
       # SPA routing - serve index.html for all routes
       location / {
           try_files $uri $uri/ /index.html;
       }
       
       # Health check endpoint
       location /health {
           access_log off;
           return 200 "OK";
           add_header Content-Type text/plain;
       }
   }
   ```

2. **Enable site and test configuration**
   ```bash
   sudo ln -s /etc/nginx/sites-available/retyrment /etc/nginx/sites-enabled/
   sudo rm /etc/nginx/sites-enabled/default
   sudo nginx -t
   sudo systemctl reload nginx
   ```

---

### Step 7: Configure SSL with Let's Encrypt

```bash
# Obtain SSL certificate
sudo certbot --nginx -d retyrment.com -d www.retyrment.com

# Auto-renewal is automatically configured
# Test renewal
sudo certbot renew --dry-run
```

---

### Step 8: Configure Google OAuth2

1. **Go to Google Cloud Console**: https://console.cloud.google.com

2. **Create OAuth 2.0 Credentials**:
   - Navigate to APIs & Services > Credentials
   - Create OAuth 2.0 Client ID
   - Application type: Web application
   
3. **Configure Authorized URLs**:
   ```
   Authorized JavaScript origins:
   - https://retyrment.com
   
   Authorized redirect URIs:
   - https://retyrment.com/api/login/oauth2/code/google
   ```

4. **Update environment file** with client ID and secret:
   ```bash
   sudo nano /opt/retyrment/.env
   # Update GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET
   
   sudo systemctl restart retyrment
   ```

---

### Step 9: Configure Firewall (UFW)

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
sudo ufw status
```

---

## Post-Deployment Checklist

### Verification Steps

- [ ] **MongoDB**: `mongosh` connects successfully
- [ ] **Backend**: `curl http://localhost:8080/api/actuator/health` returns OK
- [ ] **NGINX**: `curl http://localhost` serves frontend
- [ ] **SSL**: https://retyrment.com loads without warnings
- [ ] **OAuth**: Google login works correctly
- [ ] **API**: All endpoints respond correctly

### Test Commands

```bash
# Check services
sudo systemctl status mongod
sudo systemctl status retyrment
sudo systemctl status nginx

# Check ports
sudo netstat -tlpn | grep -E '80|443|8080|27017'

# Check logs
tail -f /var/log/retyrment/app.log
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# Test API health
curl -k https://retyrment.com/api/actuator/health
```

---

## Maintenance

### Updating the Application

```bash
# Stop service
sudo systemctl stop retyrment

# Backup current JAR
cp /opt/retyrment/retyrment-1.0.0.jar /opt/retyrment/retyrment-1.0.0.jar.bak

# Upload new JAR (from local)
scp -i your-key.pem target/retyrment-1.0.0.jar ubuntu@your-ec2-ip:/opt/retyrment/

# Start service
sudo systemctl start retyrment
sudo systemctl status retyrment
```

### Database Backup

```bash
# Create backup script
sudo nano /opt/retyrment/backup.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/opt/retyrment/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

mongodump --uri="mongodb://retyrment_app:YOUR_PASSWORD@localhost:27017/retyrment_prod?authSource=retyrment_prod" \
    --out="$BACKUP_DIR/backup_$DATE"

# Keep only last 7 days
find $BACKUP_DIR -type d -mtime +7 -exec rm -rf {} +
```

```bash
chmod +x /opt/retyrment/backup.sh

# Add to cron (daily at 2 AM)
crontab -e
# Add: 0 2 * * * /opt/retyrment/backup.sh
```

### Log Rotation

```bash
sudo nano /etc/logrotate.d/retyrment
```

```
/var/log/retyrment/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0644 ubuntu ubuntu
}
```

---

## Monitoring (Optional)

### Install CloudWatch Agent (AWS)

```bash
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb
```

### Simple Health Check Script

```bash
sudo nano /opt/retyrment/health-check.sh
```

```bash
#!/bin/bash
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/actuator/health)
if [ "$HEALTH" != "200" ]; then
    echo "Backend unhealthy! Restarting..."
    sudo systemctl restart retyrment
fi
```

```bash
chmod +x /opt/retyrment/health-check.sh

# Add to cron (every 5 minutes)
crontab -e
# Add: */5 * * * * /opt/retyrment/health-check.sh
```

---

## Troubleshooting

### Backend won't start
```bash
# Check logs
tail -100 /var/log/retyrment/error.log

# Check Java
java -version

# Test MongoDB connection
mongosh "mongodb://retyrment_app:PASSWORD@localhost:27017/retyrment_prod?authSource=retyrment_prod"
```

### NGINX errors
```bash
sudo nginx -t
tail -f /var/log/nginx/error.log
```

### OAuth not working
- Verify redirect URIs in Google Console match exactly
- Check CORS settings in backend
- Ensure HTTPS is working

### Performance issues
```bash
# Check memory
free -h

# Check disk
df -h

# Check CPU
top
```

---

## Security Recommendations

1. **Regular Updates**: `sudo apt update && sudo apt upgrade`
2. **Fail2ban**: Install to prevent brute force attacks
3. **MongoDB**: Keep authentication enabled
4. **Secrets**: Never commit secrets to git
5. **Backups**: Test restore process regularly
6. **Monitoring**: Set up alerts for downtime
7. **SSL**: Ensure auto-renewal is working

---

## Cost Estimation (AWS)

| Resource | Type | Monthly Cost (approx) |
|----------|------|----------------------|
| EC2 | t3.medium | $30-40 |
| EBS | 30GB SSD | $3 |
| Elastic IP | Static IP | $4 (if not attached) |
| Data Transfer | 50GB/month | $5 |
| **Total** | | **~$40-50/month** |

For lower costs, consider t3.small ($15/month) if traffic is low.

---

## Support

For issues, check:
1. Application logs: `/var/log/retyrment/`
2. NGINX logs: `/var/log/nginx/`
3. MongoDB logs: `/var/log/mongodb/`
4. System logs: `journalctl -xe`
