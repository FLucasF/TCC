#!/bin/bash

echo "=== Testing TEST MODE ==="
echo

# Start server in test mode
echo "Starting server in TEST MODE..."
HOTEL_TEST_MODE=true java -jar target/reservation-system-1.0.0.jar > server-test.log 2>&1 &
SERVER_PID=$!
sleep 3

# Create and confirm reservation
echo "1. Creating and confirming reservation..."
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Created reservation ID: $RESERVATION_ID"
echo "Subtotal: $(echo "$RESERVATION" | jq -r '.subtotal')"
echo "Discount: $(echo "$RESERVATION" | jq -r '.discount')"
echo "Total: $(echo "$RESERVATION" | jq -r '.total')"
echo

# Confirm reservation
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation > /dev/null
echo

# Check messages in test mode
echo "2. Messages (should be redirected to testes@pousadamarealta.com.br):"
curl -s http://localhost:7070/messages | jq '.'
echo

# Test invalid email in test mode
echo "3. Creating reservation with invalid email..."
INVALID_RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Test User",
    "guestEmail": "ana.gmail.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
INVALID_ID=$(echo "$INVALID_RES" | jq -r '.id')
curl -s -X POST http://localhost:7070/reservations/$INVALID_ID/confirmation > /dev/null
echo

echo "4. All messages (invalid email should be BLOCKED):"
curl -s http://localhost:7070/messages | jq '.[] | {to, subject: .subject[0:50], status}'
echo

# Cleanup
echo "Stopping server..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "=== Test mode tests complete ==="
