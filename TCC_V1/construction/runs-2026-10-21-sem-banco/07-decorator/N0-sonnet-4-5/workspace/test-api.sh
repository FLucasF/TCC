#!/bin/bash

BASE_URL="http://localhost:7070"

echo "Testing GET /rooms..."
curl -s "$BASE_URL/rooms" | head -c 200
echo -e "\n"

echo "Testing POST /reservations..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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
echo "$RESERVATION" | head -c 200
echo -e "\n"

RESERVATION_ID=$(echo "$RESERVATION" | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo "Created reservation ID: $RESERVATION_ID"

echo "Testing POST /reservations/$RESERVATION_ID/confirmation..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | head -c 200
echo -e "\n"

echo "Testing GET /messages..."
curl -s "$BASE_URL/messages" | head -c 500
echo -e "\n"
