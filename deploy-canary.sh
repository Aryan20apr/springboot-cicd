#!/bin/bash
set -e

ACTION=$1

usage() {
    echo "Usage: $0 {start <image-tag> [percentage]|promote <image-tag>|rollback}"
    exit 1
}

if [ -z "$ACTION" ]; then
    usage
fi

# Ensure canary.conf exists
if [ ! -f canary.conf ]; then
    cat << 'EOF' > canary.conf
upstream canary_backend {
    server app-stable:8080;
}

split_clients "${remote_addr}AAA" $upstream_variant {
    *       stable_backend;
}
EOF
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

case "$ACTION" in
    start)
        IMAGE_TAG=$2
        PERCENTAGE=${3:-10%}
        [[ "$PERCENTAGE" != *% ]] && PERCENTAGE="${PERCENTAGE}%"

        if [ -z "$IMAGE_TAG" ]; then
            echo "Error: Image tag required for start action."
            usage
        fi

        echo "=== Starting Canary Deployment ($PERCENTAGE traffic to $IMAGE_TAG) ==="
        export CANARY_TAG=$IMAGE_TAG

        docker compose pull app-canary
        docker compose up -d app-canary

        if wait_for_health "app-canary" 60; then
            echo "Applying traffic split: $PERCENTAGE -> canary, remaining -> stable"
            cat << EOF > canary.conf
upstream canary_backend {
    server app-canary:8080;
}

split_clients "\${remote_addr}AAA" \$upstream_variant {
    $PERCENTAGE     canary_backend;
    *               stable_backend;
}
EOF
            docker compose up -d nginx
            docker exec nginx-proxy nginx -s reload
            echo "Canary deployment active at $PERCENTAGE traffic."
        else
            echo "Canary deployment failed health check! Stopping canary container."
            docker compose stop app-canary
            exit 1
        fi
        ;;

    promote)
        IMAGE_TAG=$2
        if [ -z "$IMAGE_TAG" ]; then
            echo "Error: Image tag required for promote action."
            usage
        fi

        echo "=== Promoting Image $IMAGE_TAG to 100% Stable Traffic ==="
        export STABLE_TAG=$IMAGE_TAG

        docker compose pull app-stable
        docker compose up -d app-stable

        if wait_for_health "app-stable" 60; then
            echo "App stable is healthy. Reverting traffic to 100% stable."
            cat << 'EOF' > canary.conf
upstream canary_backend {
    server app-stable:8080;
}

split_clients "${remote_addr}AAA" $upstream_variant {
    *       stable_backend;
}
EOF
            docker compose up -d nginx
            docker exec nginx-proxy nginx -s reload

            echo "Stopping canary container..."
            docker compose stop app-canary
            echo "Promotion complete. 100% traffic is now on stable."
        else
            echo "Error: app-stable failed health check during promotion!"
            exit 1
        fi
        ;;

    rollback)
        echo "=== Rolling back Canary Deployment ==="
        cat << 'EOF' > canary.conf
upstream canary_backend {
    server app-stable:8080;
}

split_clients "${remote_addr}AAA" $upstream_variant {
    *       stable_backend;
}
EOF
        docker compose up -d nginx
        docker exec nginx-proxy nginx -s reload
        docker compose stop app-canary
        echo "Canary rolled back. 100% traffic on stable."
        ;;

    *)
        usage
        ;;
esac
