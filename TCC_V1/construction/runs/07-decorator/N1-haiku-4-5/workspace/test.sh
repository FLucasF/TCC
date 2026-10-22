#!/bin/bash

BASE_URL="http://localhost:7070"

echo "Testing Pousada Maré Alta API"
echo "=============================="
echo ""

# Test GET /rooms
echo "1. Testing GET /rooms"
curl -s "$BASE_URL/rooms" | jq .
echo ""

# Test POST /reservations
echo "2. Testing POST /reservations"
RESERVATION_RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_RESPONSE" | jq .
RESERVATION_ID=$(echo "$RESERVATION_RESPONSE" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID"
echo ""

# Test GET /reservations/{id}
echo "3. Testing GET /reservations/$RESERVATION_ID"
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""

# Test POST /reservations/{id}/confirmation
echo "4. Testing POST /reservations/$RESERVATION_ID/confirmation"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo ""

# Test GET /messages
echo "5. Testing GET /messages"
curl -s "$BASE_URL/messages" | jq .
echo ""

# Test POST /reservations/{id}/check-in
echo "6. Testing POST /reservations/$RESERVATION_ID/check-in"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo ""

# Test POST /reservations/{id}/check-out
echo "7. Testing POST /reservations/$RESERVATION_ID/check-out"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo ""

# Test GET /reservations/{id}/document
echo "8. Testing GET /reservations/$RESERVATION_ID/document"
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .
echo ""

# Test GET /reports/occupancy
echo "9. Testing GET /reports/occupancy?date=2027-07-10"
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo ""

# Test GET /reports/revenue
echo "10. Testing GET /reports/revenue?month=2027-07"
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo ""

echo "Tests completed!"
