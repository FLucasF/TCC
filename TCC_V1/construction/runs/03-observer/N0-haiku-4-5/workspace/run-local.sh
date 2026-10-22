#!/bin/bash

if [ -z "$DB_URL" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo "Error: Database environment variables are required"
    echo "Set: DB_URL, DB_USER, DB_PASSWORD"
    exit 1
fi

PORT=${PORT:-7070}

echo "Starting Pousada Maré Alta Reservation System..."
echo "Database: $DB_URL"
echo "Port: $PORT"

java -jar target/reservas-1.0.0.jar
