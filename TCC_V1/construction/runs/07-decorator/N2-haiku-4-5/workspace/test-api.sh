#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta API ==="
echo ""

echo "1. Getting rooms..."
curl -s $BASE_URL/rooms | jq '.[0]'
echo ""

echo "2. Creating a reservation (10/07/2027 to 12/07/2027)..."
RESPONSE=$(curl -s -X POST $BASE_URL/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo $RESPONSE | jq '.'
RESERVATION_ID=$(echo $RESPONSE | jq -r '.id')
echo ""

echo "3. Getting reservation..."
curl -s $BASE_URL/reservations/$RESERVATION_ID | jq '.'
echo ""

echo "4. Confirming reservation..."
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}' | jq '.'
echo ""

echo "5. Getting messages sent..."
curl -s $BASE_URL/messages | jq '.'
echo ""

echo "6. Check-in..."
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/check-in | jq '.status'
echo ""

echo "7. Check-out..."
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/check-out | jq '.title'
echo ""

echo "=== Test Complete ==="
