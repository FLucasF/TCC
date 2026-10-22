#!/bin/bash

# Script para testar a API da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Testing Rooms ==="
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n=== Creating Reservation ==="
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')

echo -e "\n=== Confirming Reservation ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}' | jq .

echo -e "\n=== Check-in ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" \
  -H "Content-Type: application/json" | jq .

echo -e "\n=== Check-out ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" \
  -H "Content-Type: application/json" | jq .

echo -e "\n=== Getting Document (2nd copy) ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .

echo -e "\n=== Messages Sent ==="
curl -s "$BASE_URL/messages" | jq .

echo -e "\n=== Occupancy Report ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"

echo -e "\n=== Revenue Report ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"
