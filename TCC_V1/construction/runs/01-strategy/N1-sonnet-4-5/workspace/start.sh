#!/bin/bash

# Start PostgreSQL with docker-compose
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to start..."
sleep 5

# Build and start the application
mvn clean package -DskipTests
java -jar target/pousada-reservas-1.0.0.jar
