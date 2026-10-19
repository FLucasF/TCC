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
echo "Reservation ID: $RES_ID"
echo "Subtotal: $(echo $RESP | jq '.subtotal') | Total: $(echo $RESP | jq '.total')"
echo

# Test 2: Confirm the reservation
echo "Test 2: Confirming reservation"
curl -s -X POST "$API/reservations/$RES_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq '{id, status, amountPaid}'
echo

# Test 3: Check-in and check-out
echo "Test 3: Check-in for reservation"
curl -s -X POST "$API/reservations/$RES_ID/check-in" | jq '{id, status}'
echo

echo "Test 4: Check-out and get document"
curl -s -X POST "$API/reservations/$RES_ID/check-out" | jq -r '.text'
echo

# Test 5: Create CNPJ reservation for high season
echo -e "\nTest 5: Creating CNPJ reservation (Dec 20-22, 2027, high season)"
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
echo "Reservation ID: $RES_ID2"
echo "Subtotal: $(echo $RESP2 | jq '.subtotal') | Discount: $(echo $RESP2 | jq '.discount') | Total: $(echo $RESP2 | jq '.total')"
echo

# Test 6: Confirm and get fatura
echo "Test 6: Confirming CNPJ reservation and getting fatura"
curl -s -X POST "$API/reservations/$RES_ID2/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 795.40}' | jq '{id, status}'
echo

echo "Test 7: CNPJ document (fatura)"
curl -s "$API/reservations/$RES_ID2/document" | jq -r '.text'
echo

# Test 8: Create passport reservation
echo -e "\nTest 8: Creating passport reservation"
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
echo "Reservation ID: $RES_ID3"
echo "Total: $(echo $RESP3 | jq '.total')"
echo

# Test 9: Confirm passport reservation
echo "Test 9: Confirming passport reservation"
curl -s -X POST "$API/reservations/$RES_ID3/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 1159.80}' | jq '{id, status}'
echo

echo "Test 10: Passport document (booking receipt)"
curl -s "$API/reservations/$RES_ID3/document" | jq -r '.text'
echo

# Test 11: Error cases
echo -e "\nTest 11: Room unavailable error"
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
  }' | jq '{error, message}'
echo

# Test 12: Revenue report
echo -e "\nTest 12: Revenue report for July 2027"
curl -s "$API/reports/revenue?month=2027-07"
echo

echo "=== Tests Complete ==="
