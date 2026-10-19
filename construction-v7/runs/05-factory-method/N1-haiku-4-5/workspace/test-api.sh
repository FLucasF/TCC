#!/bin/bash

API="http://localhost:7070"

echo "=== Testing Pousada Maré Alta API ==="
echo

# Test 1: Create a reservation with CPF
echo "Test 1: Creating reservation with CPF (10-12 July 2027)"
RESP=$(curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
RES_ID=$(echo $RESP | jq '.id')
echo "Created reservation ID: $RES_ID"
echo $RESP | jq '.'
echo

# Test 2: Confirm the reservation
echo "Test 2: Confirming reservation"
curl -s -X POST "$API/reservations/$RES_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq '.'
echo

# Test 3: Check messages
echo "Test 3: Checking messages sent"
curl -s "$API/messages" | jq '.[] | {channel, to, subject}'
echo

# Test 4: Get occupancy report for 10/07/2027
echo "Test 4: Occupancy report for 2027-07-10"
curl -s "$API/reports/occupancy?date=2027-07-10"
echo

# Test 5: Create a CNPJ reservation for high season
echo "Test 5: Creating CNPJ reservation (Dec 20-22, 2027)"
RESP2=$(curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@company.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-12-20",
    "checkOut": "2027-12-22",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
RES_ID2=$(echo $RESP2 | jq '.id')
echo "Created reservation ID: $RES_ID2"
echo $RESP2 | jq '.'
echo

# Test 6: Confirm CNPJ reservation
echo "Test 6: Confirming CNPJ reservation"
curl -s -X POST "$API/reservations/$RES_ID2/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 1518.80}' | jq '.{id, total, discount, status}'
echo

# Test 7: Create a passport reservation
echo "Test 7: Creating passport reservation"
RESP3=$(curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
RES_ID3=$(echo $RESP3 | jq '.id')
echo "Created reservation ID: $RES_ID3"
echo $RESP3 | jq '.{id, guestName, status, amountPaid}'
echo

# Test 8: Test error case - room not available
echo "Test 8: Testing room conflict error"
curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Another Guest",
    "guestEmail": "another@example.com",
    "guestCpf": "999.999.999-99",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.{error, message}'
echo

# Test 9: Test error case - invalid identification
echo "Test 9: Testing invalid identification error"
curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Bad Guest",
    "guestEmail": "bad@example.com",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.{error, message}'
echo

echo "=== Tests Complete ==="
