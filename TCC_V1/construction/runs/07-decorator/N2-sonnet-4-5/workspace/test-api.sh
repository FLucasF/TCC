#!/bin/bash

# Start server in background
java -jar target/reservas-1.0-SNAPSHOT.jar &
SERVER_PID=$!

# Wait for server to start
sleep 3

# Test GET /rooms
echo "Testing GET /rooms..."
curl -s http://localhost:7070/rooms | head -c 100
echo ""

# Test POST /reservations
echo -e "\nTesting POST /reservations..."
RESPONSE=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | head -c 100
echo ""

# Test POST /reservations/1/confirmation
echo -e "\nTesting POST /reservations/1/confirmation..."
curl -s -X POST http://localhost:7070/reservations/1/confirmation | head -c 100
echo ""

# Test GET /messages
echo -e "\nTesting GET /messages..."
curl -s http://localhost:7070/messages | head -c 100
echo ""

# Clean up
kill $SERVER_PID
echo -e "\nServer stopped."
