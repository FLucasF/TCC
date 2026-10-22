#!/bin/bash
# Start the hotel service
export DB_URL=jdbc:postgresql://localhost:5432/hotel_test
export DB_USER=postgres
export DB_PASSWORD=postgres
java -jar target/hotel.jar &
PID=$!
sleep 3

# Test company reservation
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 201, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
       "companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda",
       "checkIn": "2027-03-10", "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"}' \
  | python3 -m json.tool | head -20

kill $PID 2>/dev/null
