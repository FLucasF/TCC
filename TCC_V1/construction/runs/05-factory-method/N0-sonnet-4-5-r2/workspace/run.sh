#!/bin/bash

export DB_URL=${DB_URL:-jdbc:postgresql://localhost:5432/pousada}
export DB_USER=${DB_USER:-pousada}
export DB_PASSWORD=${DB_PASSWORD:-pousada123}
export PORT=${PORT:-7070}

echo "Starting Pousada Maré Alta..."
echo "Database: $DB_URL"
echo "Port: $PORT"
echo ""

java -jar target/pousada-marealta-1.0.0.jar
