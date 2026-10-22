#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta API ==="
echo

# Test 1: Get all rooms
echo "1. Getting all rooms..."
curl -s "$BASE_URL/rooms" | jq .
echo
echo

# Test 2: Create a reservation
echo "2. Creating a reservation..."
RESERVATION_RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-10",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_RESPONSE" | jq .
RESERVATION_ID=$(echo "$RESERVATION_RESPONSE" | jq .id)
echo
echo

# Test 3: Get the reservation
echo "3. Getting the reservation..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo
echo

# Test 4: Confirm the reservation
echo "4. Confirming the reservation..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq .
echo
echo

# Test 5: Check-in
echo "5. Doing check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo
echo

# Test 6: Check-out
echo "6. Doing check-out..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo
echo

# Test 7: Get messages
echo "7. Getting all messages..."
curl -s "$BASE_URL/messages" | jq .
echo
echo

# Test 8: Get occupancy report
echo "8. Getting occupancy report for 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo
echo

# Test 9: Get revenue report
echo "9. Getting revenue report for 2027-07..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo
echo

# Test 10: Get tourism tax report
echo "10. Getting tourism tax report for 2027-07..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo
echo

# Test 11: Get rate plans report
echo "11. Getting rate plans report for 2027-07..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo
