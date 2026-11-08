#!/bin/bash

# Test script for Pousada Maré Alta API

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta API ==="
echo ""

# Test 1: Get rooms
echo "1. Getting rooms..."
curl -s "$BASE_URL/rooms" | jq '.'
echo ""

# Test 2: Create a standard reservation
echo "2. Creating a standard reservation (10/03/2027-12/03/2027)..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq '.'
RES_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Reservation ID: $RES_ID"
echo ""

# Test 3: Get reservation
echo "3. Getting reservation..."
curl -s "$BASE_URL/reservations/$RES_ID" | jq '.'
echo ""

# Test 4: Confirm reservation
echo "4. Confirming reservation..."
curl -s -X POST "$BASE_URL/reservations/$RES_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }' | jq '.'
echo ""

# Test 5: Check-in
echo "5. Checking in..."
curl -s -X POST "$BASE_URL/reservations/$RES_ID/check-in" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 6: Check-out (returns document)
echo "6. Checking out..."
curl -s -X POST "$BASE_URL/reservations/$RES_ID/check-out" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 7: Get document (second copy)
echo "7. Getting document (second copy)..."
curl -s "$BASE_URL/reservations/$RES_ID/document" | jq '.'
echo ""

# Test 8: Get messages
echo "8. Getting sent messages..."
curl -s "$BASE_URL/messages" | jq '.'
echo ""

# Test 9: Occupancy report
echo "9. Getting occupancy report for 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

# Test 10: Revenue report
echo "10. Getting revenue report for 2027-03..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

# Test 11: Create corporate reservation
echo "11. Creating corporate reservation (TECNOSUL, 10/03/2027-12/03/2027)..."
CORP_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@tecnosul.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')
echo "$CORP_RES" | jq '.'
CORP_RES_ID=$(echo "$CORP_RES" | jq -r '.id')
echo "Reservation ID: $CORP_RES_ID"
echo ""

# Test 12: Weekend package reservation
echo "12. Creating weekend package reservation (12/03/2027-14/03/2027)..."
WEEKEND_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 3,
    "ratePlan": "WEEKEND_PACKAGE"
  }')
echo "$WEEKEND_RES" | jq '.'
WEEKEND_RES_ID=$(echo "$WEEKEND_RES" | jq -r '.id')
echo "Reservation ID: $WEEKEND_RES_ID"
echo ""

# Test 13: Long stay reservation
echo "13. Creating long stay reservation (01/03/2027-08/03/2027)..."
LONGSTAY_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }')
echo "$LONGSTAY_RES" | jq '.'
LONGSTAY_RES_ID=$(echo "$LONGSTAY_RES" | jq -r '.id')
echo "Reservation ID: $LONGSTAY_RES_ID"
echo ""

# Test 14: Cancel reservation
echo "14. Cancelling corporate reservation..."
curl -s -X POST "$BASE_URL/reservations/$CORP_RES_ID/cancellation" \
  -H "Content-Type: application/json" | jq '.'
echo ""

echo "=== Tests complete ==="
