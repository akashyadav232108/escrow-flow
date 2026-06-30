# Deployment guide (AWS EC2 free tier)

Deploy the monolith backend and static React build on a single EC2 instance. Suitable for demos and portfolio projects.

## Architecture (production)

```
Internet
    │
    ▼
┌─────────────────────────────────────┐
│  EC2 (Ubuntu 22.04, t2.micro)       │
│                                     │
│  Nginx :80                          │
│    ├── /      → /var/www/escrow-flow│
│    └── /api   → localhost:8080      │
│                                     │
│  Spring Boot :8080 (systemd)        │
│  MySQL :3306 (local or Docker)      │
│  Redis :6379 (local or Docker)      │
└─────────────────────────────────────┘
```

---

## Prerequisites

- AWS account (free tier eligible)
- GitHub repo pushed
- Domain optional (can use EC2 public IP)

---

## Step 1: Launch EC2

1. AMI: **Ubuntu Server 22.04 LTS**
2. Instance: **t2.micro** or **t3.micro** (free tier)
3. Storage: 8–30 GB gp2
4. Security group inbound:
   - SSH (22) — your IP only
   - HTTP (80) — 0.0.0.0/0
   - HTTPS (443) — 0.0.0.0/0 (if using TLS later)
5. Create/download key pair (`.pem`)

---

## Step 2: Initial server setup

```bash
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>

sudo apt update && sudo apt upgrade -y
sudo apt install -y git nginx openjdk-17-jdk maven
```

### Optional: Docker for MySQL + Redis

```bash
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker ubuntu
# Log out and back in
```

`docker-compose.yml` on server:

```yaml
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: escrow_flow
      MYSQL_USER: escrow
      MYSQL_PASSWORD: changeme
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "127.0.0.1:3306:3306"

  redis:
    image: redis:7-alpine
    ports:
      - "127.0.0.1:6379:6379"

volumes:
  mysql_data:
```

```bash
docker compose up -d
```

---

## Step 3: Clone and build backend

```bash
cd /opt
sudo git clone https://github.com/<you>/Escrow-Flow.git
sudo chown -R ubuntu:ubuntu Escrow-Flow
cd Escrow-Flow/backend
```

Create `/opt/Escrow-Flow/backend/application-prod.yml` (or env file):

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/escrow_flow
    username: escrow
    password: <strong-password>
  data:
    redis:
      host: 127.0.0.1
      port: 6379

app:
  jwt:
    secret: <long-random-secret>
```

Build:

```bash
./mvnw -DskipTests package
```

---

## Step 4: systemd service for backend

`/etc/systemd/system/escrow-flow.service`:

```ini
[Unit]
Description=Escrow Flow API
After=network.target docker.service

[Service]
User=ubuntu
WorkingDirectory=/opt/Escrow-Flow/backend
ExecStart=/usr/bin/java -jar target/escrow-flow-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
Restart=on-failure
EnvironmentFile=/opt/Escrow-Flow/backend/.env

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable escrow-flow
sudo systemctl start escrow-flow
sudo systemctl status escrow-flow
```

---

## Step 5: Build and deploy frontend

On server (or build locally and scp):

```bash
cd /opt/Escrow-Flow/frontend
echo "VITE_API_BASE_URL=/api" > .env
npm install
npm run build
sudo mkdir -p /var/www/escrow-flow
sudo cp -r dist/* /var/www/escrow-flow/
```

---

## Step 6: Nginx configuration

`/etc/nginx/sites-available/escrow-flow`:

```nginx
server {
    listen 80;
    server_name _;  # or your-domain.com

    root /var/www/escrow-flow;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/escrow-flow /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

Visit `http://<EC2_PUBLIC_IP>/`.

---

## Step 7: HTTPS (optional)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

Requires a domain pointing to the EC2 elastic IP.

---

## Environment variables

Never commit secrets. Use `/opt/Escrow-Flow/backend/.env`:

```env
SPRING_DATASOURCE_PASSWORD=...
APP_JWT_SECRET=...
```

Add `.env` to `.gitignore`.

---

## Deploy updates

```bash
cd /opt/Escrow-Flow
git pull origin main

# Backend
cd backend && ./mvnw -DskipTests package
sudo systemctl restart escrow-flow

# Frontend
cd ../frontend && npm install && npm run build
sudo cp -r dist/* /var/www/escrow-flow/
```

---

## Monitoring (minimal)

```bash
# Backend logs
sudo journalctl -u escrow-flow -f

# Nginx access
sudo tail -f /var/log/nginx/access.log
```

---

## Free tier tips

- One EC2 + local MySQL/Redis fits free tier if instance stays stopped when not demoing
- Elastic IP is free while attached to running instance
- RDS is **not** free tier friendly long-term — local MySQL on EC2 is fine for this project
- Snapshot EBS before major changes

---

## Smoke test checklist

- [ ] Signup creates account
- [ ] Login returns JWT
- [ ] Add funds increases balance
- [ ] Create project with milestones
- [ ] Lock funds debits wallet
- [ ] Approve credits freelancer
- [ ] Transaction history shows audit rows
- [ ] Page refresh keeps auth (token in localStorage)
