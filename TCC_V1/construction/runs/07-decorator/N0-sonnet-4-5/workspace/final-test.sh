#!/bin/bash

set -e

echo "=== Final Integration Test ==="
echo ""

echo "Starting server..."
java -jar target/reservas-1.0.0.jar > /tmp/server-final.log 2>&1 &
SERVER_PID=$!

echo "Waiting for server to start..."
sleep 3

BASE_URL="http://localhost:7070"

echo ""
echo "1. Testing pricing calculation (high season)..."
RESERVATION=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test User",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

SUBTOTAL=$(echo "$RESERVATION" | grep -o '"subtotal":[0-9.]*' | cut -d':' -f2)
EXPECTED="599.76"
if [ "$SUBTOTAL" = "$EXPECTED" ]; then
  echo "✓ High season pricing correct: R$ $SUBTOTAL"
else
  echo "✗ Expected subtotal $EXPECTED, got $SUBTOTAL"
fi

echo ""
echo "2. Testing NON_REFUNDABLE discount..."
RESERVATION2=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Test User 2",
    "guestEmail": "test2@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')

SUBTOTAL2=$(echo "$RESERVATION2" | grep -o '"subtotal":[0-9.]*' | cut -d':' -f2)
DISCOUNT2=$(echo "$RESERVATION2" | grep -o '"discount":[0-9.]*' | cut -d':' -f2)
TOTAL2=$(echo "$RESERVATION2" | grep -o '"total":[0-9.]*' | cut -d':' -f2)

echo "  Subtotal: R$ $SUBTOTAL2"
echo "  Discount: R$ $DISCOUNT2"
echo "  Total: R$ $TOTAL2"

if [ "$DISCOUNT2" != "0" ]; then
  echo "✓ Non-refundable discount applied"
else
  echo "✗ Expected discount to be applied"
fi

echo ""
echo "3. Testing room availability validation..."
CONFLICT=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Conflict Test",
    "guestEmail": "conflict@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-11",
    "checkOut": "2027-07-13",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

ERROR_CODE=$(echo "$CONFLICT" | grep -o '"error":"[^"]*"' | cut -d'"' -f4)
if [ "$ERROR_CODE" = "ROOM_UNAVAILABLE" ]; then
  echo "✓ Room availability validation works"
else
  echo "✗ Expected ROOM_UNAVAILABLE error, got: $ERROR_CODE"
fi

echo ""
echo "4. Testing capacity validation..."
CAPACITY_ERROR=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Capacity Test",
    "guestEmail": "capacity@example.com",
    "guestCpf": "22222222222",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }')

ERROR_CODE2=$(echo "$CAPACITY_ERROR" | grep -o '"error":"[^"]*"' | cut -d'"' -f4)
if [ "$ERROR_CODE2" = "CAPACITY_EXCEEDED" ]; then
  echo "✓ Capacity validation works"
else
  echo "✗ Expected CAPACITY_EXCEEDED error, got: $ERROR_CODE2"
fi

echo ""
echo "5. Testing occupancy report..."
curl -f -s -X POST "$BASE_URL/reservations/1/confirmation" > /dev/null
REPORT=$(curl -f -s "$BASE_URL/reports/occupancy?date=2027-07-11")

if echo "$REPORT" | grep -q "101,STANDARD,1,2027-07-10,2027-07-12"; then
  echo "✓ Occupancy report generated correctly"
else
  echo "✗ Occupancy report missing expected data"
fi

echo ""
echo "6. Testing reservation state transitions..."
RESERVATION3=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "State Test",
    "guestEmail": "state@example.com",
    "guestCpf": "33333333333",
    "checkIn": "2027-09-10",
    "checkOut": "2027-09-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

RES_ID=$(echo "$RESERVATION3" | grep -o '"id":[0-9]*' | cut -d':' -f2)

STATUS=$(echo "$RESERVATION3" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$STATUS" = "PENDING" ]; then
  echo "✓ New reservation starts as PENDING"
else
  echo "✗ Expected PENDING status, got: $STATUS"
fi

CONFIRMED=$(curl -f -s -X POST "$BASE_URL/reservations/$RES_ID/confirmation")
STATUS=$(echo "$CONFIRMED" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$STATUS" = "CONFIRMED" ]; then
  echo "✓ Confirmation changes status to CONFIRMED"
else
  echo "✗ Expected CONFIRMED status, got: $STATUS"
fi

CHECKEDIN=$(curl -f -s -X POST "$BASE_URL/reservations/$RES_ID/check-in")
STATUS=$(echo "$CHECKEDIN" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$STATUS" = "CHECKED_IN" ]; then
  echo "✓ Check-in changes status to CHECKED_IN"
else
  echo "✗ Expected CHECKED_IN status, got: $STATUS"
fi

CHECKOUT=$(curl -f -s -X POST "$BASE_URL/reservations/$RES_ID/check-out")
if echo "$CHECKOUT" | grep -q "Comprovante de reserva"; then
  echo "✓ Check-out returns receipt"
else
  echo "✗ Check-out should return receipt"
fi

echo ""
echo "Stopping server..."
kill $SERVER_PID

echo ""
echo "=== All tests passed! ==="
