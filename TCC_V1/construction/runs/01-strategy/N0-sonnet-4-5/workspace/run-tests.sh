#!/bin/bash

set -e

echo "Starting PostgreSQL..."
docker-compose up -d

echo "Waiting for PostgreSQL to be ready..."
sleep 3

echo "Running integration tests..."
DB_URL=jdbc:postgresql://localhost:5432/pousada_test \
DB_USER=postgres \
DB_PASSWORD=postgres \
mvn verify

echo "Stopping PostgreSQL..."
docker-compose down

echo "All tests passed!"
