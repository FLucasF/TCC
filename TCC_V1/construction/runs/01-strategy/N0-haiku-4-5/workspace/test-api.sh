#!/bin/bash

# This script tests the Pousada Maré Alta API

BASE_URL="http://localhost:7070"

echo "=== Testing GET /rooms ==="
curl -X GET "$BASE_URL/rooms" | jq .

echo -e "\n\n=== Testing POST /reservations (STANDARD) ==="
RESPONSE=$(curl -X POST "$BASE_URL/reservations" \
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
echo "$RESPONSE" | jq .
RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id')

echo -e "\n\n=== Testing GET /reservations/{id} ==="
curl -X GET "$BASE_URL/reservations/$RESERVATION_ID" | jq .

echo -e "\n\n=== Testing POST /reservations/{id}/confirmation ==="
curl -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .

echo -e "\n\n=== Testing GET /messages ==="
curl -X GET "$BASE_URL/messages" | jq .

echo -e "\n\n=== Testing POST /reservations/{id}/check-in ==="
curl -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .

echo -e "\n\n=== Testing POST /reservations/{id}/check-out ==="
curl -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .

echo -e "\n\n=== Testing GET /reports/occupancy ==="
curl -X GET "$BASE_URL/reports/occupancy?date=2027-07-10"

echo -e "\n\n=== Testing GET /reports/revenue ==="
curl -X GET "$BASE_URL/reports/revenue?month=2027-07"

echo -e "\n\nTests completed!"
