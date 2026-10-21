#!/bin/bash
set -e

echo "Starting server..."
java -jar target/marealta-1.0.0.jar > /tmp/server.log 2>&1 &
SERVER_PID=$!
sleep 3

echo "Testing API..."

# Get rooms
echo -n "1. GET /rooms... "
ROOMS=$(curl -s http://localhost:7070/rooms)
if echo "$ROOMS" | grep -q "101"; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Create reservation
echo -n "2. POST /reservations... "
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
RESERVATION_ID=$(echo "$RESERVATION" | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
if [ -n "$RESERVATION_ID" ]; then
    echo "OK (ID: $RESERVATION_ID)"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Confirm reservation
echo -n "3. POST /reservations/$RESERVATION_ID/confirmation... "
CONFIRMED=$(curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation)
if echo "$CONFIRMED" | grep -q '"status":"CONFIRMED"'; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Check messages (should have 3: guest, housekeeping, finance)
echo -n "4. GET /messages (should have 3)... "
MESSAGES=$(curl -s http://localhost:7070/messages)
MESSAGE_COUNT=$(echo "$MESSAGES" | grep -o '"id":[0-9]*' | wc -l)
if [ "$MESSAGE_COUNT" -eq 3 ]; then
    echo "OK"
else
    echo "FAIL (got $MESSAGE_COUNT messages)"
    kill $SERVER_PID
    exit 1
fi

# Verify guest email
echo -n "5. Verify guest email... "
if echo "$MESSAGES" | grep -q "carla@example.com" && \
   echo "$MESSAGES" | grep -q "Reserva confirmada - Pousada Maré Alta"; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Verify housekeeping email
echo -n "6. Verify housekeeping email... "
if echo "$MESSAGES" | grep -q "governanca@pousadamarealta.com.br" && \
   echo "$MESSAGES" | grep -q "Reserva confirmada - quarto 201"; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Verify finance email
echo -n "7. Verify finance email... "
if echo "$MESSAGES" | grep -q "financeiro@pousadamarealta.com.br" && \
   echo "$MESSAGES" | grep -q "Pagamento recebido - reserva"; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Get document
echo -n "8. GET /reservations/$RESERVATION_ID/document... "
DOCUMENT=$(curl -s http://localhost:7070/reservations/$RESERVATION_ID/document)
if echo "$DOCUMENT" | grep -q '"title":"Comprovante de reserva"'; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Cancel reservation
echo -n "9. POST /reservations/$RESERVATION_ID/cancellation... "
CANCELLED=$(curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/cancellation)
if echo "$CANCELLED" | grep -q '"status":"CANCELLED"'; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Check messages again (should have 5: 3 from confirmation + 2 from cancellation)
echo -n "10. GET /messages (should have 5)... "
MESSAGES=$(curl -s http://localhost:7070/messages)
MESSAGE_COUNT=$(echo "$MESSAGES" | grep -o '"id":[0-9]*' | wc -l)
if [ "$MESSAGE_COUNT" -eq 5 ]; then
    echo "OK"
else
    echo "FAIL (got $MESSAGE_COUNT messages)"
    kill $SERVER_PID
    exit 1
fi

# Get occupancy report
echo -n "11. GET /reports/occupancy... "
OCCUPANCY=$(curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10")
if echo "$OCCUPANCY" | grep -q "room,type,reservation,check_in,check_out"; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

# Get revenue report
echo -n "12. GET /reports/revenue... "
REVENUE=$(curl -s "http://localhost:7070/reports/revenue?month=2027-03")
if echo "$REVENUE" | grep -q "reservation,room,check_in,check_out,status,amount"; then
    echo "OK"
else
    echo "FAIL"
    kill $SERVER_PID
    exit 1
fi

echo ""
echo "All tests passed!"
kill $SERVER_PID
exit 0
