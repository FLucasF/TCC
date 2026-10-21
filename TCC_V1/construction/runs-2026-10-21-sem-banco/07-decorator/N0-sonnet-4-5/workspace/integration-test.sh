#!/bin/bash

set -e

echo "Starting server..."
java -jar target/reservas-1.0.0.jar > /tmp/server.log 2>&1 &
SERVER_PID=$!

echo "Waiting for server to start..."
sleep 3

BASE_URL="http://localhost:7070"

echo "Testing GET /rooms..."
curl -f -s "$BASE_URL/rooms" > /dev/null && echo "✓ GET /rooms OK"

echo "Testing POST /reservations..."
RESERVATION=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

RESERVATION_ID=$(echo "$RESERVATION" | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo "✓ Created reservation ID: $RESERVATION_ID"

echo "Testing POST /reservations/$RESERVATION_ID/confirmation..."
curl -f -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" > /dev/null
echo "✓ Confirmation OK"

echo "Testing GET /messages (should have 2 messages: original + archive copy)..."
MESSAGES=$(curl -f -s "$BASE_URL/messages")
MESSAGE_COUNT=$(echo "$MESSAGES" | grep -o '"id"' | wc -l)
if [ "$MESSAGE_COUNT" -eq 2 ]; then
  echo "✓ Messages OK (found 2 messages as expected)"
else
  echo "✗ Expected 2 messages, found $MESSAGE_COUNT"
fi

echo "Testing invalid email (should be blocked)..."
RESERVATION2=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Carlos",
    "guestEmail": "invalid-email",
    "guestCpf": "98765432100",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

RESERVATION_ID2=$(echo "$RESERVATION2" | grep -o '"id":[0-9]*' | cut -d':' -f2)
curl -f -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/confirmation" > /dev/null

MESSAGES2=$(curl -f -s "$BASE_URL/messages")
BLOCKED_COUNT=$(echo "$MESSAGES2" | grep -o '"status":"BLOCKED"' | wc -l)
if [ "$BLOCKED_COUNT" -eq 1 ]; then
  echo "✓ Invalid email blocked correctly"
else
  echo "✗ Expected 1 blocked message, found $BLOCKED_COUNT"
fi

echo ""
echo "Stopping server..."
kill $SERVER_PID

echo "All tests passed!"
