#!/bin/bash

set -e

# Build
echo "Building project..."
mvn clean package -q

# Set defaults
DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/pousada}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-senha123}"
PORT="${PORT:-7070}"

# Display info
echo "================================================"
echo "Pousada Maré Alta - Sistema de Reservas"
echo "================================================"
echo "Starting application..."
echo "Database: $DB_URL"
echo "Port: $PORT"
echo ""
echo "API endpoints:"
echo "  GET     /rooms"
echo "  POST    /reservations"
echo "  GET     /reservations/{id}"
echo "  POST    /reservations/{id}/confirmation"
echo "  POST    /reservations/{id}/cancellation"
echo "  POST    /reservations/{id}/check-in"
echo "  POST    /reservations/{id}/check-out"
echo "  GET     /reservations/{id}/document"
echo "  GET     /messages"
echo "  GET     /reports/occupancy?date=YYYY-MM-DD"
echo "  GET     /reports/revenue?month=YYYY-MM"
echo ""
echo "For testing examples, see TESTING.md"
echo "================================================"
echo ""

export DB_URL=$DB_URL
export DB_USER=$DB_USER
export DB_PASSWORD=$DB_PASSWORD
export PORT=$PORT

java -jar target/app.jar
