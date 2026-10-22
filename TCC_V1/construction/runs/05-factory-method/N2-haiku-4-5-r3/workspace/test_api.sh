#!/bin/bash

# Test script para a API de Reservas da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Testing Rooms API ==="
curl -s "${BASE_URL}/rooms" | jq '.'
echo ""

echo "=== Creating Reservation (CPF) ==="
RESPONSE=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | jq '.'
RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID"
echo ""

echo "=== Getting Reservation ==="
curl -s "${BASE_URL}/reservations/${RESERVATION_ID}" | jq '.'
echo ""

echo "=== Confirming Reservation ==="
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/confirmation" | jq '.'
echo ""

echo "=== Checking Messages ==="
curl -s "${BASE_URL}/messages" | jq '.'
echo ""

echo "=== Checking In ==="
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/check-in" | jq '.'
echo ""

echo "=== Checking Out ==="
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/check-out" | jq '.'
echo ""

echo "=== Getting Document ==="
curl -s "${BASE_URL}/reservations/${RESERVATION_ID}/document" | jq '.text'
echo ""

echo "=== Occupancy Report for 2027-03-10 ==="
curl -s "${BASE_URL}/reports/occupancy?date=2027-03-10"
echo ""

echo "=== Revenue Report for 2027-03 ==="
curl -s "${BASE_URL}/reports/revenue?month=2027-03"
