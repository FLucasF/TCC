#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testing Pousada Maré Alta Reservation System ==="
echo ""

echo "1. Getting all rooms..."
curl -s -X GET "$BASE_URL/rooms" | jq .
echo ""

echo "2. Creating a reservation (with CPF)..."
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | jq .
RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID"
echo ""

echo "3. Confirming the reservation..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}' | jq .
echo ""

echo "4. Getting reservation details..."
curl -s -X GET "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""

echo "5. Getting confirmation message..."
curl -s -X GET "$BASE_URL/messages" | jq .
echo ""

echo "6. Check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo ""

echo "7. Check-out..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo ""

echo "8. Getting occupancy report for a date..."
curl -s -X GET "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""
echo ""

echo "9. Getting revenue report for a month..."
curl -s -X GET "$BASE_URL/reports/revenue?month=2027-03"
echo ""
echo ""

echo "=== Creating a reservation with company (CNPJ) ==="
RESPONSE2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Ana Souza",
    "guestEmail": "ana@empresa.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE2" | jq .
RESERVATION_ID2=$(echo "$RESPONSE2" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID2"
echo ""

echo "Confirming company reservation..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 389.90}' | jq .
echo ""

echo "Getting document..."
curl -s -X GET "$BASE_URL/reservations/$RESERVATION_ID2/document" | jq .
echo ""

echo "=== Creating a reservation with passport (International guest) ==="
RESPONSE3=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESPONSE3" | jq .
RESERVATION_ID3=$(echo "$RESPONSE3" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID3"
echo ""

echo "Confirming international reservation..."
AMOUNT=$(echo "$RESPONSE3" | jq -r '.total')
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID3/confirmation" \
  -H "Content-Type: application/json" \
  -d "{\"amountPaid\": $AMOUNT}" | jq .
echo ""

echo "Getting booking receipt..."
curl -s -X GET "$BASE_URL/reservations/$RESERVATION_ID3/document" | jq .
echo ""

echo "=== All tests completed ==="
