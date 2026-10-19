#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testing Hotel Reservation API ==="
echo

echo "1. GET /rooms"
curl -s "$BASE_URL/rooms" | jq '.'
echo
echo

echo "2. POST /reservations (creating reservation)"
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo
echo

echo "3. GET /reservations/$RESERVATION_ID"
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq '.'
echo
echo

echo "4. POST /reservations/$RESERVATION_ID/confirmation"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo
echo

echo "5. GET /messages"
curl -s "$BASE_URL/messages" | jq '.'
echo
echo

echo "6. GET /reservations/$RESERVATION_ID/document"
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq '.'
echo
echo

echo "7. POST /reservations/$RESERVATION_ID/check-in"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.'
echo
echo

echo "8. POST /reservations/$RESERVATION_ID/check-out"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq '.'
echo
echo

echo "9. GET /reports/occupancy?date=2027-07-10"
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo
echo

echo "10. GET /reports/revenue?month=2027-07"
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo
echo

echo "=== Tests completed ==="
