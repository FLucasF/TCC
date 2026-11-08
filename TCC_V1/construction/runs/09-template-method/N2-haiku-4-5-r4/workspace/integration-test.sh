#!/bin/bash

# Integration test for Pousada Maré Alta API
# This script tests a complete booking workflow

BASE_URL="http://localhost:7070"
ERRORS=0

test_endpoint() {
    local method=$1
    local path=$2
    local body=$3
    local expected_status=$4
    local description=$5

    echo -n "Testing: $description... "

    if [ -z "$body" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$path")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$path" \
            -H "Content-Type: application/json" \
            -d "$body")
    fi

    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "$expected_status" ]; then
        echo "✓ (Status: $http_code)"
        echo "$body"
    else
        echo "✗ Expected $expected_status, got $http_code"
        echo "$body"
        ERRORS=$((ERRORS+1))
    fi
    echo ""
}

echo "=== Pousada Maré Alta - Integration Test ==="
echo ""

# Test 1: Get rooms
test_endpoint "GET" "/rooms" "" "200" "GET /rooms"

# Test 2: Create a reservation
RESERVATION_DATA='{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
}'

RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
    -H "Content-Type: application/json" \
    -d "$RESERVATION_DATA")
RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id // empty')

if [ -z "$RESERVATION_ID" ]; then
    echo "✗ Failed to create reservation"
    echo "$RESPONSE"
    ERRORS=$((ERRORS+1))
else
    echo "✓ Created reservation with ID: $RESERVATION_ID"
    echo "$RESPONSE" | jq '.'
    echo ""

    # Test 3: Get reservation
    test_endpoint "GET" "/reservations/$RESERVATION_ID" "" "200" "GET /reservations/$RESERVATION_ID"

    # Test 4: Confirm reservation
    CONFIRM_DATA='{"amountPaid": 599.76}'
    test_endpoint "POST" "/reservations/$RESERVATION_ID/confirmation" "$CONFIRM_DATA" "200" "POST /reservations/$RESERVATION_ID/confirmation"

    # Test 5: Check-in
    test_endpoint "POST" "/reservations/$RESERVATION_ID/check-in" "" "200" "POST /reservations/$RESERVATION_ID/check-in"

    # Test 6: Get document
    test_endpoint "GET" "/reservations/$RESERVATION_ID/document" "" "200" "GET /reservations/$RESERVATION_ID/document"

    # Test 7: Check-out
    test_endpoint "POST" "/reservations/$RESERVATION_ID/check-out" "" "200" "POST /reservations/$RESERVATION_ID/check-out"
fi

# Test 8: Get messages
test_endpoint "GET" "/messages" "" "200" "GET /messages"

# Test 9: Get occupancy report
test_endpoint "GET" "/reports/occupancy?date=2027-07-10" "" "200" "GET /reports/occupancy"

# Test 10: Get revenue report
test_endpoint "GET" "/reports/revenue?month=2027-07" "" "200" "GET /reports/revenue"

# Test 11: Get tourism tax report
test_endpoint "GET" "/reports/tourism-tax?month=2027-07" "" "200" "GET /reports/tourism-tax"

# Test 12: Get rate plans report
test_endpoint "GET" "/reports/rate-plans?month=2027-07" "" "200" "GET /reports/rate-plans"

# Test 13: Error cases
test_endpoint "GET" "/reservations/999999" "" "404" "GET /reservations/999999 (not found)"

test_endpoint "POST" "/reservations" '{"roomNumber": 999}' "400" "POST /reservations (invalid data)"

echo ""
echo "=== Test Summary ==="
if [ $ERRORS -eq 0 ]; then
    echo "All tests passed! ✓"
    exit 0
else
    echo "$ERRORS test(s) failed! ✗"
    exit 1
fi
