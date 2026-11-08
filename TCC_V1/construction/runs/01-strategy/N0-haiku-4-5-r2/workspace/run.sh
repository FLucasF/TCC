#!/bin/bash

# Start PostgreSQL with Docker Compose
docker-compose up -d

# Wait for PostgreSQL to start
echo "Waiting for PostgreSQL to start..."
sleep 3

# Run the application
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="admin"
export DB_PASSWORD="admin123"
export PORT="7070"

java -jar target/mare-alta.jar
