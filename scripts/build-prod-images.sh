#!/usr/bin/env bash
set -euo pipefail

compose_file="${COMPOSE_FILE:-docker-compose.prod.yml}"
env_file="${ENV_FILE:-.env.prod}"

services=(
  eureka-service
  api-gateway-service
  auth-service
  profile-service
  job-service
  application-service
  interview-service
  notification-service
  payment-service
  hireconnect-web
  frontend
)

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export COMPOSE_PARALLEL_LIMIT=1

for service in "${services[@]}"; do
  echo "Building ${service}"
  docker compose --env-file "${env_file}" -f "${compose_file}" build --progress=plain "${service}"
done
