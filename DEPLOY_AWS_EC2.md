# HireConnect AWS EC2 Deployment

This guide deploys HireConnect on one Ubuntu EC2 instance with Docker Compose.

Compose starts the Angular frontend, API Gateway, Eureka, auth, profile, job, application, interview, notification, payment, legacy `hireconnect-web`, MySQL, Redis, and RabbitMQ.

## 1. Create The EC2 Server

Use the AWS Console:

- Region: `ap-south-1` for Mumbai, or your preferred region
- AMI: Ubuntu Server 24.04 LTS
- Instance type: `t3.large` minimum
- Storage: 30GB gp3 minimum
- Key pair: create/download a `.pem`
- Security group inbound rules:
  - SSH `22` from your IP only
  - HTTP `80` from anywhere
  - HTTPS `443` from anywhere, if you add SSL
  - API Gateway `8080` from anywhere while testing, optional after Nginx works

Allocate an Elastic IP and associate it with the instance.

## 2. Install Docker On EC2

```bash
ssh -i your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP

sudo apt update
sudo apt install -y git curl
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker

docker --version
docker compose version
```

## 3. Clone The Repo

```bash
git clone YOUR_GITHUB_REPO_URL HireConnect
cd HireConnect
```

## 4. Create Production Env File

```bash
cp .env.prod.example .env.prod
nano .env.prod
```

Set these carefully:

- `APP_PUBLIC_URL`
- `FRONTEND_ALLOWED_ORIGINS`
- `PUBLIC_BASE_URL`
- `OAUTH2_DEFAULT_SUCCESS_URL`
- `MYSQL_ROOT_PASSWORD`
- `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`

Use your EC2 IP first:

```env
APP_PUBLIC_URL=http://YOUR_EC2_PUBLIC_IP
FRONTEND_ALLOWED_ORIGINS=http://YOUR_EC2_PUBLIC_IP
PUBLIC_BASE_URL=http://YOUR_EC2_PUBLIC_IP
OAUTH2_DEFAULT_SUCCESS_URL=http://YOUR_EC2_PUBLIC_IP/oauth2/callback
```

When you move to a domain, change those values to `https://your-domain.com`.

## 5. Build And Start

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml build
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

## 6. Check Logs

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f eureka-service
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f api-gateway-service
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f auth-service
```

## 7. Test In Browser

Open:

```text
http://YOUR_EC2_PUBLIC_IP
http://YOUR_EC2_PUBLIC_IP:8080/actuator/health
http://YOUR_EC2_PUBLIC_IP:8761
```

After the gateway and services are registered in Eureka, test login/register from the frontend.

## 8. Useful Commands

Restart everything:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml restart
```

Pull latest code and redeploy:

```bash
git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml build
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

Stop:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

Stop and delete database/cache volumes:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down -v
```

## 9. Domain And SSL Later

Point your domain A record to the Elastic IP. Then update `.env.prod`:

```env
APP_PUBLIC_URL=https://your-domain.com
FRONTEND_ALLOWED_ORIGINS=https://your-domain.com
PUBLIC_BASE_URL=https://your-domain.com
OAUTH2_DEFAULT_SUCCESS_URL=https://your-domain.com/oauth2/callback
```

For GitHub OAuth, add this callback URL in the GitHub OAuth app:

```text
https://your-domain.com/login/oauth2/code/github
```

For IP-only testing, use:

```text
http://YOUR_EC2_PUBLIC_IP/login/oauth2/code/github
```
