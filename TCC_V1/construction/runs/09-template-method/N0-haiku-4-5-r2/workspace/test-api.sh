#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta Reservation System ==="

# Test 1: List rooms
echo -e "\n1. Listing rooms..."
curl -s "$BASE_URL/rooms" | jq . | head -20

# Test 2: Create a reservation
echo -e "\n2. Creating a reservation..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Created reservation ID: $RESERVATION_ID"

# Test 3: Get reservation
echo -e "\n3. Getting reservation details..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .

# Test 4: Confirm reservation
echo -e "\n4. Confirming reservation..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq .

# Test 5: Check-in
echo -e "\n5. Check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" \
  -H "Content-Type: application/json" | jq .

# Test 6: Get document
echo -e "\n6. Getting receipt document..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .

# Test 7: Get messages
echo -e "\n7. Getting sent messages..."
curl -s "$BASE_URL/messages" | jq .

# Test 8: Occupancy report
echo -e "\n8. Getting occupancy report for 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"

# Test 9: Revenue report
echo -e "\n\n9. Getting revenue report for 2027-07..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"

# Test 10: Tourism tax report
echo -e "\n\n10. Getting tourism tax report for 2027-07..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"

# Test 11: Rate plans report
echo -e "\n\n11. Getting rate plans report for 2027-07..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"

echo -e "\n\n=== Tests completed ==="
