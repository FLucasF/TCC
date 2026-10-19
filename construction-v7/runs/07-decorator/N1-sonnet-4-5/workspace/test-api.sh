#!/bin/bash

echo "=== Testing Pousada Maré Alta API ==="
echo

# Start server in background
echo "Starting server..."
java -jar target/reservation-system-1.0.0.jar > server.log 2>&1 &
SERVER_PID=$!
sleep 3

# Test 1: Get rooms
echo "1. GET /rooms"
curl -s http://localhost:7070/rooms | jq '.' || echo "FAILED"
echo

# Test 2: Create reservation
echo "2. POST /reservations (creating reservation)"
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Created reservation ID: $RESERVATION_ID"
echo

# Test 3: Confirm reservation
echo "3. POST /reservations/$RESERVATION_ID/confirmation"
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation | jq '.'
echo

# Test 4: Get messages
echo "4. GET /messages (should show confirmation email)"
curl -s http://localhost:7070/messages | jq '.'
echo

# Test 5: Get document
echo "5. GET /reservations/$RESERVATION_ID/document"
curl -s http://localhost:7070/reservations/$RESERVATION_ID/document | jq '.'
echo

# Test 6: Occupancy report
echo "6. GET /reports/occupancy?date=2027-03-10"
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10"
echo
echo

# Test 7: Revenue report
echo "7. GET /reports/revenue?month=2027-03"
curl -s "http://localhost:7070/reports/revenue?month=2027-03"
echo
echo

# Test 8: Invalid email (should be blocked)
echo "8. POST /reservations with invalid email (should be blocked)"
INVALID_RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Test User",
    "guestEmail": "invalid-email",
    "guestCpf": "99988877766",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
echo "$INVALID_RES" | jq '.'
INVALID_ID=$(echo "$INVALID_RES" | jq -r '.id')
curl -s -X POST http://localhost:7070/reservations/$INVALID_ID/confirmation > /dev/null
echo "Messages after invalid email confirmation:"
curl -s http://localhost:7070/messages | jq 'map(select(.to == "invalid-email"))'
echo

# Cleanup
echo "Stopping server..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "=== Tests complete ==="
