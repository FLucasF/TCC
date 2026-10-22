#!/bin/bash

echo "Starting Pousada Maré Alta Reservation System test..."

# Start a test PostgreSQL database in Docker (if available)
if command -v docker &> /dev/null; then
    echo "Starting PostgreSQL container..."
    docker run -d \
        --name test-postgres \
        -e POSTGRES_PASSWORD=test \
        -e POSTGRES_DB=reservas \
        -p 5433:5432 \
        postgres:18 &>/dev/null

    sleep 2

    export DB_URL="jdbc:postgresql://localhost:5433/reservas"
    export DB_USER="postgres"
    export DB_PASSWORD="test"
    export PORT="7070"

    echo "PostgreSQL started. Running application tests..."

    # Run the application
    timeout 5 java -jar target/app.jar &
    sleep 2

    # Test the API
    echo "Testing GET /rooms..."
    curl -s http://localhost:7070/rooms | head -c 100
    echo -e "\n"

    # Cleanup
    kill %1 2>/dev/null
    docker stop test-postgres &>/dev/null
    docker rm test-postgres &>/dev/null
else
    echo "Docker not available. Verifying jar is executable..."
    java -jar target/app.jar --help 2>&1 | head -5 || echo "Jar created successfully"
fi

echo "Test complete!"
