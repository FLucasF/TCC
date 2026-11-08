#!/bin/bash

# Example client script to test Pousada Maré Alta API
# Make sure the server is running on http://localhost:7070

BASE_URL="http://localhost:7070"

echo "=== Pousada Maré Alta - Client Example ==="
echo ""

# Get rooms
echo "1. Getting available rooms..."
curl -s "$BASE_URL/rooms" | jq .
echo ""

# Create a reservation
echo "2. Creating a reservation..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq '.id')
echo "Reservation ID: $RESERVATION_ID"
echo ""

# Get reservation details
echo "3. Getting reservation details..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""

# Confirm reservation
echo "4. Confirming reservation..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}' | jq .
echo ""

# Check-in
echo "5. Checking in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo ""

# Check-out
echo "6. Checking out..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo ""

# Get messages
echo "7. Getting sent messages..."
curl -s "$BASE_URL/messages" | jq .
echo ""

# Get occupancy report
echo "8. Getting occupancy report for 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo ""
echo ""

# Get revenue report
echo "9. Getting revenue report for 2027-07..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo ""
echo ""

# Get tourism tax report
echo "10. Getting tourism tax report for 2027-07..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo ""
echo ""

# Get rate plans report
echo "11. Getting rate plans report for 2027-07..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo ""
