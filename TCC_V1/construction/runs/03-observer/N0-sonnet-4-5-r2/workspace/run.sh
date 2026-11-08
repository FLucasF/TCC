#!/bin/bash
set -e

echo "Starting PostgreSQL..."
docker-compose up -d postgres

echo "Waiting for PostgreSQL to be ready..."
until docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
  sleep 1
done

echo "PostgreSQL is ready!"
echo ""
echo "Starting Pousada Maré Alta Reservation System..."
echo ""

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT="7070"

java -jar target/reservation-system-1.0.0.jar
