#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta API ==="
echo ""

# Test 1: GET /rooms
echo "1. Testing GET /rooms"
curl -s "$BASE_URL/rooms" | jq '.' || echo "Failed"
echo ""

# Test 2: POST /reservations
echo "2. Testing POST /reservations"
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
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Created reservation with ID: $RESERVATION_ID"
echo ""

# Test 3: GET /reservations/{id}
echo "3. Testing GET /reservations/$RESERVATION_ID"
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq '.'
echo ""

# Test 4: POST /reservations/{id}/confirmation
echo "4. Testing POST /reservations/$RESERVATION_ID/confirmation"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq '.'
echo ""

# Test 5: GET /messages
echo "5. Testing GET /messages"
curl -s "$BASE_URL/messages" | jq '.'
echo ""

# Test 6: GET /reports/occupancy
echo "6. Testing GET /reports/occupancy?date=2027-07-11"
curl -s "$BASE_URL/reports/occupancy?date=2027-07-11"
echo ""
echo ""

# Test 7: GET /reports/revenue
echo "7. Testing GET /reports/revenue?month=2027-07"
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo ""
echo ""

# Test 8: GET /reports/tourism-tax
echo "8. Testing GET /reports/tourism-tax?month=2027-07"
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo ""
echo ""

# Test 9: GET /reports/rate-plans
echo "9. Testing GET /reports/rate-plans?month=2027-07"
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo ""
echo ""

echo "=== API Tests Complete ==="
