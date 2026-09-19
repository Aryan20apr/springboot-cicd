#!/bin/bash
set -e

IMAGE_TAG=$1
if [ -z "$IMAGE_TAG" ]; then
    echo "Usage: $0 <image-tag>"
    exit 1
fi

COMPOSE_FILE="docker-compose.staging.yaml"

wait_for_health() {
    local SERVICE=$1
    local TIMEOUT=${2:-60}
    echo "Waiting for $SERVICE to become healthy (timeout: ${TIMEOUT}s)..."
    local END=$((SECONDS+TIMEOUT))
    while [ $SECONDS -lt $END ]; do
        local STATUS
        STATUS=$(docker inspect --format='{{json .State.Health.Status}}' "$SERVICE" 2>/dev/null | tr -d '"')
        if [ "$STATUS" == "healthy" ]; then
            echo "$SERVICE is healthy!"
            return 0
        fi
        sleep 2
    done
    echo "Error: $SERVICE failed healthcheck!"
    return 1
}

export IMAGE_TAG=$IMAGE_TAG

# Cold start: ensure staging postgres is up and healthy
if ! docker ps --format '{{.Names}}' | grep -q "^staging-postgres-db$"; then
    echo "=== Starting Staging PostgreSQL ==="
    docker compose -f $COMPOSE_FILE up -d postgres-staging
    wait_for_health "staging-postgres-db" 60
fi

# Cold start: if either staging replica is not running, bootstrap both first
if ! docker ps --format '{{.Names}}' | grep -q "^staging-app-1$" || \
   ! docker ps --format '{{.Names}}' | grep -q "^staging-app-2$"; then
    echo "=== Cold Start: Bootstrapping staging replicas with $IMAGE_TAG ==="
    docker compose -f $COMPOSE_FILE pull app-1-staging app-2-staging
    docker compose -f $COMPOSE_FILE up -d --remove-orphans app-1-staging app-2-staging
    wait_for_health "staging-app-1" 60
    wait_for_health "staging-app-2" 60
    docker compose -f $COMPOSE_FILE up -d nginx-staging
    docker exec staging-nginx-proxy nginx -s reload || true
    echo "Cold start complete. Both staging replicas are healthy."
    exit 0
fi

echo "=== Pulling new image ($IMAGE_TAG) for staging ==="
docker compose -f $COMPOSE_FILE pull app-1-staging app-2-staging

# Step 1: Update staging app-1
echo "=== Step 1/2: Rolling update for staging-app-1 ==="
docker compose -f $COMPOSE_FILE up -d --no-deps --remove-orphans app-1-staging

if ! wait_for_health "staging-app-1" 60; then
    echo "Error: staging-app-1 failed health check! staging-app-2 is still serving traffic."
    docker compose -f $COMPOSE_FILE stop app-1-staging
    exit 1
fi
echo "staging-app-1 is healthy and serving traffic!"

# Step 2: Update staging app-2
echo "=== Step 2/2: Rolling update for staging-app-2 ==="
docker compose -f $COMPOSE_FILE up -d --no-deps --remove-orphans app-2-staging

if ! wait_for_health "staging-app-2" 60; then
    echo "Error: staging-app-2 failed health check! staging-app-1 is serving traffic."
    docker compose -f $COMPOSE_FILE stop app-2-staging
    exit 1
fi
echo "staging-app-2 is healthy and serving traffic!"

# Ensure nginx-staging is up and reloaded
docker compose -f $COMPOSE_FILE up -d nginx-staging
docker exec staging-nginx-proxy nginx -s reload || true

# Prune dangling images
docker image prune -f || true

echo "=== Staging rolling deployment completed successfully! ==="
