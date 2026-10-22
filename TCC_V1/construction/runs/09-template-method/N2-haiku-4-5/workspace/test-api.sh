#!/bin/bash

# Sample API test script for Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Getting all rooms ==="
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n=== Creating a reservation ==="
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

RES_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Created reservation: $RES_ID"

echo -e "\n=== Getting reservation details ==="
curl -s "$BASE_URL/reservations/$RES_ID" | jq .

echo -e "\n=== Confirming reservation ==="
curl -s -X POST "$BASE_URL/reservations/$RES_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}' | jq .

echo -e "\n=== Checking in ==="
curl -s -X POST "$BASE_URL/reservations/$RES_ID/check-in" | jq .

echo -e "\n=== Checking out ==="
curl -s -X POST "$BASE_URL/reservations/$RES_ID/check-out" | jq .

echo -e "\n=== Getting receipt (second copy) ==="
curl -s "$BASE_URL/reservations/$RES_ID/document" | jq .

echo -e "\n=== Getting sent messages ==="
curl -s "$BASE_URL/messages" | jq .

echo -e "\n=== Occupancy report for 2027-03-10 ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"

echo -e "\n=== Revenue report for 2027-03 ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"

echo -e "\n=== Tourism tax report for 2027-03 ==="
curl -s "$BASE_URL/reports/tourism-tax?month=2027-03"

echo -e "\n=== Rate plans report for 2027-03 ==="
curl -s "$BASE_URL/reports/rate-plans?month=2027-03"
