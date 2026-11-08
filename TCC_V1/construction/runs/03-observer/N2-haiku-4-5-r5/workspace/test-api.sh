#!/bin/bash

set -e

BASE_URL="${1:-http://localhost:7070}"
echo "Testing API at $BASE_URL"

# Test 1: Get rooms
echo -e "\n=== Test 1: GET /rooms ==="
curl -s "$BASE_URL/rooms" | jq '.'

# Test 2: Create a reservation
echo -e "\n=== Test 2: POST /reservations ==="
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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

echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq '.id')
echo "Created reservation with ID: $RESERVATION_ID"

# Test 3: Get reservation
echo -e "\n=== Test 3: GET /reservations/{id} ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq '.'

# Test 4: Confirm reservation
echo -e "\n=== Test 4: POST /reservations/{id}/confirmation ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.8}' | jq '.'

# Test 5: Get messages
echo -e "\n=== Test 5: GET /messages ==="
curl -s "$BASE_URL/messages" | jq '.'

# Test 6: Get occupancy report
echo -e "\n=== Test 6: GET /reports/occupancy ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-11"

# Test 7: Get revenue report
echo -e "\n=== Test 7: GET /reports/revenue ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"

# Test 8: Get document (receipt)
echo -e "\n=== Test 8: GET /reservations/{id}/document ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq '.'

# Test 9: Check-in
echo -e "\n=== Test 9: POST /reservations/{id}/check-in ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.'

# Test 10: Check-out (returns document)
echo -e "\n=== Test 10: POST /reservations/{id}/check-out ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.'

echo -e "\n=== All tests completed ==="
