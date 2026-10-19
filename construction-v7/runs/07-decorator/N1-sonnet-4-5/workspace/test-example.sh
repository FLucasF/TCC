#!/bin/bash

echo "=== Testing specific example from requirements ==="
echo "STANDARD room from 10/07/2027 to 12/07/2027 (high season)"
echo "Expected: 2 nights of R$ 299,88 = R$ 599,76 total"
echo

java -jar target/reservation-system-1.0.0.jar > server.log 2>&1 &
SERVER_PID=$!
sleep 3

RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test User",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "Result:"
echo "$RESERVATION" | jq '{nights, subtotal, discount, total}'
echo

kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "✓ Test complete"
