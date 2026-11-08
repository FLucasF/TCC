#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. GET /rooms"
curl -s $BASE_URL/rooms | jq .
echo
echo

echo "2. POST /reservations - Criar uma reserva"
RESERVATION=$(curl -s -X POST $BASE_URL/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo $RESERVATION | jq .
RESERVATION_ID=$(echo $RESERVATION | jq -r '.id')
echo "ID da reserva criada: $RESERVATION_ID"
echo

echo "3. GET /reservations/{id} - Obter detalhes da reserva"
curl -s $BASE_URL/reservations/$RESERVATION_ID | jq .
echo
echo

echo "4. POST /reservations/{id}/confirmation - Confirmar reserva"
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }' | jq .
echo
echo

echo "5. GET /messages - Ver emails enviados"
curl -s $BASE_URL/messages | jq .
echo
echo

echo "6. POST /reservations/{id}/check-in - Check-in"
curl -s -X POST $BASE_URL/reservations/$RESERVATION_ID/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
echo
echo

echo "7. GET /reports/occupancy?date=2027-07-10"
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo
echo

echo "8. GET /reports/revenue?month=2027-07"
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo
echo

echo "9. GET /reports/tourism-tax?month=2027-07"
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo
echo

echo "10. GET /reports/rate-plans?month=2027-07"
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo
