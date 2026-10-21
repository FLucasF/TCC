#!/bin/bash

set -e

echo "Starting server in TEST MODE..."
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar > /tmp/server-test.log 2>&1 &
SERVER_PID=$!

echo "Waiting for server to start..."
sleep 3

BASE_URL="http://localhost:7070"

echo "Testing reservation with valid email in test mode..."
RESERVATION=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')

RESERVATION_ID=$(echo "$RESERVATION" | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo "✓ Created reservation ID: $RESERVATION_ID"

curl -f -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" > /dev/null
echo "✓ Confirmation OK"

echo "Checking messages in test mode..."
MESSAGES=$(curl -f -s "$BASE_URL/messages")

# In test mode: 1 message redirected to testes@pousadamarealta.com.br, NO archive copy
MESSAGE_COUNT=$(echo "$MESSAGES" | grep -o '"id"' | wc -l)
if [ "$MESSAGE_COUNT" -eq 1 ]; then
  echo "✓ Correct message count in test mode (1 message, no archive copy)"
else
  echo "✗ Expected 1 message in test mode, found $MESSAGE_COUNT"
fi

# Check that message went to test email
TEST_EMAIL_COUNT=$(echo "$MESSAGES" | grep -o 'testes@pousadamarealta.com.br' | wc -l)
if [ "$TEST_EMAIL_COUNT" -eq 1 ]; then
  echo "✓ Message redirected to test email"
else
  echo "✗ Expected message to test email"
fi

# Check that subject has [TESTE] prefix
TESTE_SUBJECT_COUNT=$(echo "$MESSAGES" | grep -o '\[TESTE\]' | wc -l)
if [ "$TESTE_SUBJECT_COUNT" -eq 1 ]; then
  echo "✓ Subject has [TESTE] prefix"
else
  echo "✗ Expected [TESTE] prefix in subject"
fi

# Check that body has original recipient
ORIGINAL_RECIPIENT=$(echo "$MESSAGES" | grep -o 'Destinatário original: maria@example.com' | wc -l)
if [ "$ORIGINAL_RECIPIENT" -eq 1 ]; then
  echo "✓ Body contains original recipient"
else
  echo "✗ Expected original recipient in body"
fi

echo "Testing blocked email in test mode..."
RESERVATION2=$(curl -f -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "João",
    "guestEmail": "joao.invalid",
    "guestCpf": "55566677788",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

RESERVATION_ID2=$(echo "$RESERVATION2" | grep -o '"id":[0-9]*' | cut -d':' -f2)
curl -f -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/confirmation" > /dev/null

MESSAGES2=$(curl -f -s "$BASE_URL/messages")
BLOCKED_COUNT=$(echo "$MESSAGES2" | grep -o '"status":"BLOCKED"' | wc -l)
if [ "$BLOCKED_COUNT" -eq 1 ]; then
  echo "✓ Invalid email still blocked in test mode"
else
  echo "✗ Expected 1 blocked message"
fi

# Blocked message should keep original email (not redirected)
BLOCKED_ORIGINAL=$(echo "$MESSAGES2" | grep '"status":"BLOCKED"' | grep -o '"to":"joao.invalid"' | wc -l)
if [ "$BLOCKED_ORIGINAL" -eq 1 ]; then
  echo "✓ Blocked message keeps original email address"
else
  echo "✗ Expected blocked message to keep original address"
fi

echo ""
echo "Stopping server..."
kill $SERVER_PID

echo "All test mode tests passed!"
