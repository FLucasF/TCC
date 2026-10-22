#!/bin/bash

set -e

echo "Starting PostgreSQL with Docker Compose..."
docker-compose up -d

echo "Waiting for PostgreSQL to be ready..."
sleep 5

echo "Setting up environment variables..."
export DB_URL="jdbc:postgresql://localhost:5432/pousada_mare_alta"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT="7070"

echo "Starting Pousada Maré Alta Reservation System..."
echo "  DB_URL: $DB_URL"
echo "  Port: $PORT"
echo ""
echo "Available endpoints:"
echo "  GET  http://localhost:7070/rooms"
echo "  POST http://localhost:7070/reservations"
echo ""

java -jar target/reservation-system.jar
