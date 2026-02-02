# Deploy to Hetzner VPS Plan

Deploy super-doodle to Hetzner VPS using GitHub Actions with GraalVM native binary.

## Architecture

```
[GitHub] --push--> [GitHub Actions] --SSH--> [Hetzner VPS]
                        |                          |
                   Build:                     Run:
                   - Frontend (Vite)          - nginx (static + proxy)
                   - Reference DB (SQLite)    - systemd (backend binary)
                   - Backend (GraalVM)        - wahapedia-ref.db (bundled)
                                              - wahapedia-user.db (persistent)
```

## Prerequisites

- Hetzner VPS with Ubuntu/Debian
- Domain configured to point to VPS IP
- SSH access with key-based authentication

## GitHub Secrets Required

| Secret | Description |
|--------|-------------|
| `VPS_HOST` | VPS IP address or hostname |
| `VPS_USER` | SSH username (e.g., `deploy`) |
| `VPS_SSH_KEY` | SSH private key for deployment |
| `DOMAIN` | Domain name for nginx/SSL |

## Files to Create

### 1. GitHub Actions Workflow

**`.github/workflows/deploy.yml`**

```yaml
name: Deploy

on:
  push:
    branches: [master]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Frontend build
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: cd frontend && npm ci && npm run build

      # Reference DB build
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: sbt/setup-sbt@v1
      - run: cd backend && sbt "runMain wahapedia.BuildRefDb ../data/wahapedia wahapedia-ref.db"

      # Backend native image build
      - uses: graalvm/setup-graalvm@v1
        with:
          java-version: '17'
          distribution: 'graalvm'
      - run: cd backend && sbt nativeImage

      # Deploy via SSH
      - name: Deploy to VPS
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            sudo systemctl stop super-doodle || true

      - name: Copy files
        uses: appleboy/scp-action@v0
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          source: "frontend/dist/*,backend/target/native-image/backend,backend/wahapedia-ref.db"
          target: "/opt/super-doodle"
          strip_components: 1

      - name: Start service
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            sudo systemctl start super-doodle
```

### 2. Nginx Configuration

**`deploy/nginx.conf`**

```nginx
server {
    listen 80;
    server_name YOUR_DOMAIN;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name YOUR_DOMAIN;

    ssl_certificate /etc/letsencrypt/live/YOUR_DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/YOUR_DOMAIN/privkey.pem;

    root /opt/super-doodle/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. Systemd Service

**`deploy/super-doodle.service`**

```ini
[Unit]
Description=Super Doodle Army Builder
After=network.target

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/super-doodle
Environment="REF_DB_PATH=/opt/super-doodle/wahapedia-ref.db"
Environment="USER_DB_PATH=/opt/super-doodle/wahapedia-user.db"
ExecStart=/opt/super-doodle/backend
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### 4. VPS Setup Script

**`deploy/setup-vps.sh`**

```bash
#!/bin/bash
set -e

# Run as root on fresh VPS

# Create deploy user
useradd -m -s /bin/bash deploy
mkdir -p /home/deploy/.ssh
# Add your SSH public key here
echo "YOUR_SSH_PUBLIC_KEY" >> /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys

# Allow deploy user to restart service
echo "deploy ALL=(ALL) NOPASSWD: /bin/systemctl start super-doodle, /bin/systemctl stop super-doodle, /bin/systemctl restart super-doodle" >> /etc/sudoers.d/deploy

# Install nginx
apt update
apt install -y nginx certbot python3-certbot-nginx

# Create app directory
mkdir -p /opt/super-doodle/frontend
chown -R deploy:deploy /opt/super-doodle

# Install systemd service
cp deploy/super-doodle.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable super-doodle

# Configure nginx
cp deploy/nginx.conf /etc/nginx/sites-available/super-doodle
ln -sf /etc/nginx/sites-available/super-doodle /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

# Get SSL certificate (interactive)
certbot --nginx -d YOUR_DOMAIN
```

## Deployment Flow

1. Push to `master` triggers workflow
2. GitHub Actions builds:
   - Frontend: `npm run build` → `frontend/dist/`
   - Reference DB: `sbt runMain wahapedia.BuildRefDb` → `wahapedia-ref.db`
   - Backend: `sbt nativeImage` → native binary
3. SCP artifacts to VPS `/opt/super-doodle/`
4. Restart systemd service
5. User DB (`wahapedia-user.db`) is NOT touched - persists across deploys

## Directory Structure on VPS

```
/opt/super-doodle/
├── frontend/              # Static files from Vite build
│   ├── index.html
│   └── assets/
├── backend                # GraalVM native binary
├── wahapedia-ref.db       # Reference data (replaced on deploy)
└── wahapedia-user.db      # User data (persists across deploys)
```

## First-Time Setup

1. Provision Hetzner VPS (Ubuntu 22.04 recommended)
2. Point domain DNS to VPS IP
3. SSH to VPS as root
4. Run `setup-vps.sh`
5. Configure GitHub secrets
6. Push to master to trigger first deploy

## Verification

1. Check GitHub Actions workflow completes
2. SSH to VPS:
   - `systemctl status super-doodle` - service running
   - `curl localhost:8080/api/factions` - backend responds
3. Browser: https://your-domain.com loads app
4. Test: register, login, create army
