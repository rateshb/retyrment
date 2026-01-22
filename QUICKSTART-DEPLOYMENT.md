# Quick Start Deployment Guide

This guide will help you deploy the Retyrment application on AWS EC2 in **under 30 minutes**.

## Prerequisites

- AWS Account
- Domain name (optional but recommended)
- SSH client
- Basic command line knowledge

---

## Step 1: Launch EC2 Instance (5 minutes)

1. **Login to AWS Console** → EC2 → Launch Instance

2. **Configure Instance:**
   ```
   Name: retyrment-prod
   AMI: Ubuntu Server 22.04 LTS
   Instance Type: t2.medium
   Key Pair: Create new or select existing
   Storage: 30 GB gp3
   ```

3. **Security Group:**
   ```
   SSH (22)    - Your IP
   HTTP (80)   - 0.0.0.0/0
   HTTPS (443) - 0.0.0.0/0
   ```

4. **Launch Instance**

5. **Allocate Elastic IP:**
   - EC2 → Elastic IPs → Allocate
   - Associate with your instance
   - Note down the IP address

---

## Step 2: Setup Server (10 minutes)

1. **Connect to server:**
   ```bash
   chmod 400 your-key.pem
   ssh -i your-key.pem ubuntu@YOUR-ELASTIC-IP
   ```

2. **Download and run setup script:**
   ```bash
   cd ~
   wget https://raw.githubusercontent.com/your-repo/retyrment/main/scripts/setup-server.sh
   sudo bash setup-server.sh
   ```

   This installs:
   - Java 17
   - Maven
   - Node.js 20
   - NGINX
   - MongoDB
   - Certbot (for SSL)

3. **Configure environment variables:**
   ```bash
   sudo cp /etc/environment.d/retyrment.conf.template /etc/environment.d/retyrment.conf
   sudo nano /etc/environment.d/retyrment.conf
   ```

   Fill in:
   ```bash
   export MONGODB_URI="mongodb://localhost:27017/retyrment"
   # Or for MongoDB Atlas: mongodb+srv://username:password@cluster.mongodb.net/retyrment
   export JWT_SECRET="$(openssl rand -base64 32)"
   export GOOGLE_CLIENT_ID="your_client_id.apps.googleusercontent.com"
   export GOOGLE_CLIENT_SECRET="your_client_secret"
   ```

4. **Configure MongoDB (optional for local setup):**
   ```bash
   # MongoDB is ready to use without auth for development
   # For production, enable authentication in /etc/mongod.conf
   ```

---

## Step 3: Clone and Build Application (5 minutes)

1. **Clone repository:**
   ```bash
   cd /home/ubuntu
   git clone https://github.com/your-username/retyrment.git
   cd retyrment
   ```

2. **Update backend configuration:**
   ```bash
   nano backend/src/main/resources/application-prod.yml
   ```

   Update:
   ```yaml
   app:
     frontend-url: https://retyrment.com  # or http://YOUR-ELASTIC-IP
     cors:
       allowed-origins: https://retyrment.com,https://www.retyrment.com
   
   spring:
     security:
       oauth2:
         client:
           registration:
             google:
               redirect-uri: https://retyrment.com/api/oauth2/callback
   ```

3. **Update frontend API URLs:**
   
   **React:**
   ```bash
   nano frontend-react/.env.production
   ```
   ```
   VITE_API_URL=https://retyrment.com/api
   ```

   **VanillaJS:**
   ```bash
   nano frontend/js/api.js
   ```
   ```javascript
   const API_BASE = 'https://retyrment.com/api';
   ```

4. **Deploy application:**
   ```bash
   chmod +x scripts/deploy.sh
   sudo bash scripts/deploy.sh --all
   ```

   This will:
   - Build backend JAR
   - Build React frontend
   - Copy frontends to NGINX
   - Start backend service

---

## Step 4: Configure NGINX (5 minutes)

1. **Update NGINX configuration:**
   ```bash
   sudo cp /etc/nginx/sites-available/retyrment.template /etc/nginx/sites-available/retyrment
   sudo nano /etc/nginx/sites-available/retyrment
   ```

   Replace `yourdomain.com` with `retyrment.com` (or use IP for testing)

2. **Enable site:**
   ```bash
   sudo ln -s /etc/nginx/sites-available/retyrment /etc/nginx/sites-enabled/
   sudo rm /etc/nginx/sites-enabled/default
   sudo nginx -t
   sudo systemctl reload nginx
   ```

---

## Step 5: Setup SSL (5 minutes)

### If you have a domain:

1. **Update DNS records** (in your domain registrar):
   ```
   A Record: @ → YOUR-ELASTIC-IP
   A Record: www → YOUR-ELASTIC-IP
   ```

2. **Wait for DNS propagation** (check with `nslookup retyrment.com`)

3. **Get SSL certificate:**
   ```bash
   sudo certbot --nginx -d retyrment.com -d www.retyrment.com
   ```

   Follow the prompts:
   - Enter email address
   - Agree to Terms of Service
   - Choose to redirect HTTP to HTTPS

### If testing without domain:

- Access via `http://YOUR-ELASTIC-IP`
- Skip SSL setup for now
- Add SSL later when you have a domain

---

## Step 6: Configure Google OAuth (3 minutes)

1. **Go to [Google Cloud Console](https://console.cloud.google.com)**

2. **Navigate to:** APIs & Services → Credentials

3. **Edit your OAuth 2.0 Client ID**

4. **Add Authorized JavaScript origins:**
   ```
   https://retyrment.com
   https://www.retyrment.com
   ```

5. **Add Authorized redirect URIs:**
   ```
   https://retyrment.com/api/oauth2/callback
   https://www.retyrment.com/api/oauth2/callback
   ```

6. **Save**

---

## Step 7: Verify Deployment

1. **Check services:**
   ```bash
   /home/ubuntu/monitor.sh
   ```

   Expected output:
   ```
   Backend: active
   NGINX: active
   PostgreSQL: active
   ```

2. **Test URLs:**
   ```bash
   # Health check
   curl https://retyrment.com/health
   
   # API health
   curl https://retyrment.com/api/health
   ```

3. **Access application:**
   - VanillaJS: `https://retyrment.com`
   - React: `https://retyrment.com/react`

4. **Test login:**
   - Click "Login with Google"
   - Complete OAuth flow
   - Verify you're logged in

---

## Quick Commands Reference

### Service Management
```bash
# Start/Stop/Restart backend
sudo systemctl start retyrment-backend
sudo systemctl stop retyrment-backend
sudo systemctl restart retyrment-backend

# View backend logs
sudo journalctl -u retyrment-backend -f

# Restart NGINX
sudo systemctl reload nginx
```

### Deployment
```bash
# Deploy backend only
sudo bash scripts/deploy.sh --backend

# Deploy frontends only
sudo bash scripts/deploy.sh --frontend

# Deploy everything
sudo bash scripts/deploy.sh --all
```

### Monitoring
```bash
# Quick status check
/home/ubuntu/monitor.sh

# View NGINX logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# View backend logs
sudo journalctl -u retyrment-backend -n 100

# System resources
htop
df -h
free -h
```

### Backup
```bash
# Manual backup
/home/ubuntu/backup.sh

# Setup automatic daily backups at 2 AM
crontab -e
```
Add:
```
0 2 * * * /home/ubuntu/backup.sh >> /home/ubuntu/backup.log 2>&1
```

---

## Troubleshooting

### Backend won't start
```bash
# Check logs
sudo journalctl -u retyrment-backend -n 50

# Common fixes:
# 1. Database connection - verify credentials
# 2. Port conflict - check if 8080 is in use
sudo lsof -i :8080

# 3. Environment variables not loaded
sudo systemctl restart retyrment-backend
```

### NGINX 502 Bad Gateway
```bash
# Check if backend is running
sudo systemctl status retyrment-backend

# Test backend directly
curl http://localhost:8080/api/health

# Check NGINX error logs
sudo tail -f /var/log/nginx/error.log
```

### Login not working
1. Verify Google OAuth redirect URIs match exactly
2. Check if frontend URL in `application-prod.yml` is correct
3. View browser console for errors
4. Check backend logs for OAuth errors

### SSL certificate errors
```bash
# Check certificate status
sudo certbot certificates

# Renew certificate
sudo certbot renew

# Test renewal
sudo certbot renew --dry-run
```

---

## Post-Deployment Checklist

- [ ] Application accessible via domain
- [ ] SSL certificate installed and auto-renewal working
- [ ] Google OAuth login working
- [ ] Both frontends (VanillaJS and React) accessible
- [ ] Backend API responding
- [ ] Database connected
- [ ] Logs rotating properly
- [ ] Backup script scheduled
- [ ] Firewall configured
- [ ] DNS records correct

---

## Next Steps

1. **Set up monitoring:**
   - Install monitoring tool (e.g., Prometheus + Grafana)
   - Set up alerts for downtime

2. **Configure backups:**
   - Set up S3 bucket for backups
   - Configure automated backup uploads

3. **Performance tuning:**
   - Enable NGINX caching
   - Adjust JVM heap size based on usage
   - Consider using AWS RDS for database

4. **Security hardening:**
   - Enable fail2ban
   - Configure AWS WAF
   - Set up log monitoring

---

## Support

For detailed documentation, see [DEPLOYMENT.md](DEPLOYMENT.md)

For issues:
1. Check logs first
2. Review troubleshooting section
3. Search GitHub issues
4. Create new issue with logs

---

**Total Time:** ~30 minutes
**Difficulty:** Beginner to Intermediate

🎉 **Congratulations! Your Retyrment application is now live!**
