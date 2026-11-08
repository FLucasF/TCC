#!/bin/bash

# Test script for Pousada Maré Alta API

BASE_URL="http://localhost:7070"

echo "Testing Pousada Maré Alta API..."
echo ""

# Test 1: Get rooms
echo "1. Testing GET /rooms"
curl -s $BASE_URL/rooms | jq '.' || echo "FAILED"
echo ""

# Test 2: Create reservation
echo "2. Testing POST /reservations"
RESERVATION=$(curl -s -X POST $BASE_URL/reservations \
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

echo $RESERVATION | jq '.'
RESERVATION_ID=$(echo $RESERVATION | jq -r '.id')
echo "Created reservation with ID: $RESERVATION_ID"
echo ""

# Test 3: Get reservation
echo "3. Testing GET /reservations/{id}"
curl -s $BASE_URL/reservations/$RESERVATION_ID | jq '.'
echo ""

# Test 4: Confirm reservation
echo "4. Testing POST /reservations/{id}/confirmation"
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/confirmation | jq '.'
echo ""

# Test 5: Get messages
echo "5. Testing GET /messages"
curl -s $BASE_URL/messages | jq '.'
echo ""

# Test 6: Check-in
echo "6. Testing POST /reservations/{id}/check-in"
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/check-in | jq '.'
echo ""

# Test 7: Get document
echo "7. Testing GET /reservations/{id}/document"
curl -s $BASE_URL/reservations/$RESERVATION_ID/document | jq '.'
echo ""

# Test 8: Check-out
echo "8. Testing POST /reservations/{id}/check-out"
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/check-out | jq '.'
echo ""

# Test 9: Occupancy report
echo "9. Testing GET /reports/occupancy"
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

# Test 10: Revenue report
echo "10. Testing GET /reports/revenue"
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "Tests completed!"
