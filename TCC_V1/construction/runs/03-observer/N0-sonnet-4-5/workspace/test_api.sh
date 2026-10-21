#!/bin/bash

echo "Testing API endpoints..."

# Start server in background
PORT=7070 java -jar target/reservas-1.0-SNAPSHOT.jar > /dev/null 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 3

# Test GET /rooms
echo "Testing GET /rooms..."
curl -s http://localhost:7070/rooms | head -100

# Test POST /reservations
echo -e "\n\nTesting POST /reservations..."
RESERVATION_JSON=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION_JSON" | head -100

# Get reservation ID
RESERVATION_ID=$(echo "$RESERVATION_JSON" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo -e "\n\nReservation ID: $RESERVATION_ID"

# Test POST /reservations/{id}/confirmation
echo -e "\nTesting POST /reservations/${RESERVATION_ID}/confirmation..."
curl -s -X POST http://localhost:7070/reservations/${RESERVATION_ID}/confirmation | head -100

# Test GET /messages
echo -e "\n\nTesting GET /messages..."
curl -s http://localhost:7070/messages | head -200

# Kill server
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo -e "\n\nDone!"
