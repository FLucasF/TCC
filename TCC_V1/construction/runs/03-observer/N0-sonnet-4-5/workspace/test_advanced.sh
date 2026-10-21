#!/bin/bash

echo "Testing advanced features..."

# Start server in background
PORT=7070 java -jar target/reservas-1.0-SNAPSHOT.jar > /dev/null 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 3

# Create and confirm a reservation
echo "Creating reservation..."
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')

RESERVATION_ID=$(echo "$RESERVATION" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "Created reservation $RESERVATION_ID"
echo "$RESERVATION"

# Confirm
echo -e "\n\nConfirming..."
curl -s -X POST http://localhost:7070/reservations/${RESERVATION_ID}/confirmation | head -100

# Test occupancy report
echo -e "\n\nTesting occupancy report for 2027-07-10..."
curl -s "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Test revenue report
echo -e "\n\nTesting revenue report for 2027-07..."
curl -s "http://localhost:7070/reports/revenue?month=2027-07"

# Cancel the reservation
echo -e "\n\nCancelling reservation..."
curl -s -X POST http://localhost:7070/reservations/${RESERVATION_ID}/cancellation | head -100

# Check messages after cancellation
echo -e "\n\nMessages after cancellation..."
curl -s http://localhost:7070/messages | tail -200

# Kill server
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo -e "\n\nDone!"
