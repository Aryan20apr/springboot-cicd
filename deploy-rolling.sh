#!/bin/bash
set -e

IMAGE_TAG=$1
if [ -z "$IMAGE_TAG" ]; then
    echo "Usage: $0 <image-tag>"
    exit 1
fi

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

# Cold start check: if either app-1 or app-2 is not running, bootstrap both first
if ! docker ps --format '{{.Names}}' | grep -q "^app-1$" || ! docker ps --format '{{.Names}}' | grep -q "^app-2$"; then
    echo "=== Cold Start: Bootstrapping both replicas (app-1 and app-2) with $IMAGE_TAG ==="
    docker compose pull app-1 app-2
    docker compose up -d --remove-orphans app-1 app-2
    wait_for_health "app-1" 60
    wait_for_health "app-2" 60
    docker compose up -d nginx
    docker exec nginx-proxy nginx -s reload || true
    echo "Cold start complete. Both replicas are healthy and taking traffic."
    exit 0
fi

echo "=== Pulling new image ($IMAGE_TAG) ==="
docker compose pull app-1 app-2

# Step 1: Update app-1
echo "=== Step 1/2: Rolling update for app-1 ==="
docker compose up -d --no-deps --remove-orphans app-1

if ! wait_for_health "app-1" 60; then
    echo "Error: app-1 failed health check! Stopping app-1. app-2 is still serving traffic."
    docker compose stop app-1
    exit 1
fi
echo "app-1 is healthy and serving traffic!"

# Step 2: Update app-2
echo "=== Step 2/2: Rolling update for app-2 ==="
docker compose up -d --no-deps --remove-orphans app-2

if ! wait_for_health "app-2" 60; then
    echo "Error: app-2 failed health check! Stopping app-2. app-1 is serving traffic."
    docker compose stop app-2
    exit 1
fi
echo "app-2 is healthy and serving traffic!"

# Ensure nginx is up and configuration reloaded
docker compose up -d nginx
docker exec nginx-proxy nginx -s reload || true

# Prune dangling images
docker image prune -f || true

echo "=== Rolling deployment completed successfully! ==="
