#!/bin/bash

BASE_URL="http://localhost:7070"
FAILED=0

test_endpoint() {
    local method=$1
    local path=$2
    local data=$3
    local expected_code=$4

    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$path" -H "Content-Type: application/json")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$path" -H "Content-Type: application/json" -d "$data")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    echo "Testing: $method $path"
    echo "Expected: $expected_code, Got: $http_code"

    if [ "$http_code" -eq "$expected_code" ]; then
        echo "✓ PASS"
    else
        echo "✗ FAIL"
        echo "Response: $body"
        FAILED=$((FAILED + 1))
    fi
    echo ""
}

echo "Waiting for server to start..."
for i in {1..30}; do
    if curl -s "$BASE_URL/rooms" > /dev/null 2>&1; then
        echo "Server is ready!"
        break
    fi
    sleep 1
done

echo "Running API tests..."
echo ""

# Test 1: Get rooms
test_endpoint GET "/rooms" "" 200

# Test 2: Create reservation
reservation_data='{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
}'
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reservations" \
    -H "Content-Type: application/json" \
    -d "$reservation_data")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
echo "Testing: POST /reservations"
echo "Expected: 201, Got: $http_code"
if [ "$http_code" -eq 201 ]; then
    echo "✓ PASS"
    reservation_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
    echo "Created reservation ID: $reservation_id"
else
    echo "✗ FAIL"
    echo "Response: $body"
    FAILED=$((FAILED + 1))
fi
echo ""

# Test 3: Get reservation
if [ ! -z "$reservation_id" ]; then
    test_endpoint GET "/reservations/$reservation_id" "" 200
fi

# Test 4: Get messages
test_endpoint GET "/messages" "" 200

# Test 5: Get occupancy report
test_endpoint GET "/reports/occupancy?date=2027-07-10" "" 200

# Test 6: Get revenue report
test_endpoint GET "/reports/revenue?month=2027-07" "" 200

# Test 7: Invalid room number
test_endpoint POST "/reservations" '{"roomNumber": 999, "guestName": "Test", "guestEmail": "test@test.com", "guestCpf": "123.456.789-00", "checkIn": "2027-07-10", "checkOut": "2027-07-12", "guests": 1, "ratePlan": "STANDARD"}' 404

echo ""
echo "Test Summary: $FAILED failed tests"
exit $FAILED
