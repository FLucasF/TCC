#!/bin/bash

# Test script for Pousada Maré Alta API
# This script tests the main use cases mentioned by the owner

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta API ==="
echo ""

# Test 1: Get rooms
echo "1. Getting all rooms..."
curl -s "$BASE_URL/rooms" | jq '.'
echo ""

# Test 2: Create STANDARD reservation
echo "2. Creating STANDARD reservation (10/03/2027 to 12/03/2027)..."
STANDARD_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
echo "$STANDARD_RES" | jq '.'
STANDARD_ID=$(echo "$STANDARD_RES" | jq '.id')
echo "Reservation ID: $STANDARD_ID"
echo ""

# Test 3: Create CORPORATE reservation (TECNOSUL)
echo "3. Creating CORPORATE reservation (TECNOSUL, 10/03/2027 to 12/03/2027)..."
CORPORATE_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233345",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')
echo "$CORPORATE_RES" | jq '.'
CORPORATE_ID=$(echo "$CORPORATE_RES" | jq '.id')
echo ""

# Test 4: Create WEEKEND_PACKAGE
echo "4. Creating WEEKEND_PACKAGE (12/03/2027 to 14/03/2027)..."
WEEKEND_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@email.com",
    "guestCpf": "11122233346",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }')
echo "$WEEKEND_RES" | jq '.'
WEEKEND_ID=$(echo "$WEEKEND_RES" | jq '.id')
echo ""

# Test 5: Create LONG_STAY
echo "5. Creating LONG_STAY (01/03/2027 to 08/03/2027)..."
LONGSTAY_RES=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Pedro Oliveira",
    "guestEmail": "pedro@email.com",
    "guestCpf": "11122233347",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }')
echo "$LONGSTAY_RES" | jq '.'
LONGSTAY_ID=$(echo "$LONGSTAY_RES" | jq '.id')
echo ""

# Test 6: Confirm STANDARD reservation
echo "6. Confirming STANDARD reservation..."
curl -s -X POST "$BASE_URL/reservations/$STANDARD_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}' | jq '.'
echo ""

# Test 7: Confirm CORPORATE reservation
echo "7. Confirming CORPORATE reservation..."
curl -s -X POST "$BASE_URL/reservations/$CORPORATE_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 686.22}' | jq '.'
echo ""

# Test 8: Confirm WEEKEND_PACKAGE
echo "8. Confirming WEEKEND_PACKAGE..."
curl -s -X POST "$BASE_URL/reservations/$WEEKEND_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 820.00}' | jq '.'
echo ""

# Test 9: Check-in
echo "9. Doing check-in on STANDARD reservation..."
curl -s -X POST "$BASE_URL/reservations/$STANDARD_ID/check-in" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 10: Check-out
echo "10. Doing check-out on STANDARD reservation..."
curl -s -X POST "$BASE_URL/reservations/$STANDARD_ID/check-out" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 11: Get document
echo "11. Getting document for STANDARD reservation..."
curl -s "$BASE_URL/reservations/$STANDARD_ID/document" | jq '.'
echo ""

# Test 12: Get messages
echo "12. Getting all messages (emails sent)..."
curl -s "$BASE_URL/messages" | jq '.'
echo ""

# Test 13: Occupancy report
echo "13. Getting occupancy report for 10/03/2027..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

# Test 14: Revenue report
echo "14. Getting revenue report for March 2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "=== Tests completed ==="
