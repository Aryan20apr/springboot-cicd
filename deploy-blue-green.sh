#!/bin/bash
set -e

IMAGE_TAG=$1
if [ -z "$IMAGE_TAG" ]; then
    echo "Usage: $0 <image-tag>"
    exit 1
fi

# Ensure upstream.conf exists (default to blue as active for initial setup)
if [ ! -f upstream.conf ]; then
    echo "upstream backend { server app-blue:8080; }" > upstream.conf
fi

# Determine active color by reading the upstream config
if grep -q "app-blue" upstream.conf; then
    ACTIVE="blue"
    IDLE="green"
else
    ACTIVE="green"
    IDLE="blue"
fi

echo "Current active is $ACTIVE. Deploying to $IDLE..."

# Export the tag for the idle environment
export ${IDLE^^}_IMAGE_TAG=$IMAGE_TAG

# Start the idle container
docker compose pull app-$IDLE
docker compose up -d app-$IDLE

# Wait for healthcheck to pass (Timeout after 60s)
echo "Waiting for app-$IDLE to become healthy..."
END=$((SECONDS+60))
HEALTHY=false
while [ $SECONDS -lt $END ]; do
    STATUS=$(docker inspect --format='{{json .State.Health.Status}}' app-$IDLE | tr -d '"')
    if [ "$STATUS" == "healthy" ]; then
        HEALTHY=true
        break
    fi
    sleep 2
done

if [ "$HEALTHY" = true ]; then
    echo "App is healthy! Switching traffic to $IDLE."
    echo "upstream backend { server app-$IDLE:8080; }" > upstream.conf
    
    # Ensure nginx container is running and reload configuration
    docker compose up -d nginx
    docker exec nginx-proxy nginx -s reload
    
    # Optional: Stop the old active container to save resources
    docker compose stop app-$ACTIVE
    echo "Deployment successful."
else
    echo "Healthcheck failed! Rolling back (keeping $ACTIVE active)."
    docker compose stop app-$IDLE
    exit 1
fi
