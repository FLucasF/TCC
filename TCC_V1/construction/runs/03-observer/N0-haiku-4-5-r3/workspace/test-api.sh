#!/bin/bash

set -e

DB_CONTAINER_ID=""

cleanup() {
    if [ -n "$DB_CONTAINER_ID" ]; then
        echo "Stopping database container..."
        docker stop "$DB_CONTAINER_ID" || true
        docker rm "$DB_CONTAINER_ID" || true
    fi
    if [ -n "$APP_PID" ]; then
        kill "$APP_PID" || true
    fi
}

trap cleanup EXIT

echo "Starting PostgreSQL container..."
DB_CONTAINER_ID=$(docker run -d \
    -e POSTGRES_DB=pousada \
    -e POSTGRES_USER=pousada \
    -e POSTGRES_PASSWORD=pousada123 \
    -p 5432:5432 \
    postgres:18)

echo "Waiting for database to be ready..."
sleep 5

echo "Starting application..."
export PORT=7070
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"

java -jar /workspace/target/pousada-system.jar &
APP_PID=$!

sleep 3

echo ""
echo "===== Testing Rooms API ====="
curl -s http://localhost:7070/rooms | jq .

echo ""
echo "===== Creating Reservation ====="
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .

RES_ID=$(echo "$RESERVATION" | jq -r '.id')

echo ""
echo "===== Confirming Reservation ====="
AMOUNT=$(echo "$RESERVATION" | jq -r '.total')
curl -s -X POST "http://localhost:7070/reservations/$RES_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d "{\"amountPaid\": $AMOUNT}" | jq .

echo ""
echo "===== Getting Messages ====="
curl -s http://localhost:7070/messages | jq '.[] | {to: .to, subject: .subject}'

echo ""
echo "===== Getting Occupancy Report ====="
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10"

echo ""
echo "===== Tests completed successfully! ====="
