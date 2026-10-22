#!/bin/bash

BASE_URL="http://localhost:7070"

echo "Testing Pousada Maré Alta API"
echo "=============================="
echo ""

# Test 1: Get rooms
echo "1. Testing GET /rooms"
curl -s "$BASE_URL/rooms" | jq .
echo ""

# Test 2: Create a reservation
echo "2. Creating a STANDARD reservation (July, high season)"
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | jq .
RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID"
echo ""

# Test 3: Get the reservation
echo "3. Getting reservation details"
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""

# Test 4: Confirm the reservation
echo "4. Confirming reservation"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq .
echo ""

# Test 5: Check in
echo "5. Performing check-in"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
echo ""

# Test 6: Check out
echo "6. Performing check-out"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
echo ""

# Test 7: Get document (second copy)
echo "7. Getting document (second copy)"
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .
echo ""

# Test 8: Get messages
echo "8. Getting sent messages"
curl -s "$BASE_URL/messages" | jq .
echo ""

# Test 9: Occupancy report
echo "9. Getting occupancy report for 2027-07-11"
curl -s "$BASE_URL/reports/occupancy?date=2027-07-11"
echo ""

# Test 10: Revenue report
echo "10. Getting revenue report for 2027-07"
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo ""

echo "Tests completed!"
