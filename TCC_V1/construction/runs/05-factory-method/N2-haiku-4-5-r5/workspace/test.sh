#!/bin/bash

set -e

echo "Starting PostgreSQL..."
docker-compose up -d postgres
sleep 5

echo "Building application..."
mvn clean package -q

echo "Starting application..."
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="pousada" \
DB_PASSWORD="pousada123" \
PORT=7070 \
java -jar target/pousada-mare-alta.jar &

APP_PID=$!
sleep 3

echo "Testing API..."

# Test 1: List rooms
echo "Test 1: List rooms"
curl -s http://localhost:7070/rooms | jq .

# Test 2: Create reservation
echo -e "\nTest 2: Create reservation (CPF)"
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .

RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')

# Test 3: Get reservation
echo -e "\nTest 3: Get reservation"
curl -s http://localhost:7070/reservations/$RESERVATION_ID | jq .

# Test 4: Confirm reservation
echo -e "\nTest 4: Confirm reservation"
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation | jq .

# Test 5: Get messages (should have confirmation email)
echo -e "\nTest 5: Get messages"
curl -s http://localhost:7070/messages | jq .

# Test 6: Create another reservation to test occupancy
echo -e "\nTest 6: Create another reservation"
RESERVATION2=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tech Corp",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')

echo "$RESERVATION2" | jq .

RESERVATION_ID2=$(echo "$RESERVATION2" | jq -r '.id')

# Test 7: Confirm second reservation
echo -e "\nTest 7: Confirm second reservation"
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID2/confirmation | jq .

# Test 8: Check-in
echo -e "\nTest 8: Check-in"
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-in | jq .

# Test 9: Check-out and get document
echo -e "\nTest 9: Check-out"
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-out | jq .

# Test 10: Get occupancy report
echo -e "\nTest 10: Get occupancy report"
curl -s "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Test 11: Get revenue report
echo -e "\n\nTest 11: Get revenue report"
curl -s "http://localhost:7070/reports/revenue?month=2027-07"

echo -e "\n\nCleaning up..."
kill $APP_PID
docker-compose down

echo "Tests completed!"
