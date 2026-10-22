#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq .
echo

echo "2. Criando reserva..."
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | jq .
RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id')
echo "ID da reserva criada: $RESERVATION_ID"
echo

echo "3. Confirmando reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo

echo "4. Verificando emails enviados..."
curl -s "$BASE_URL/messages" | jq '.[] | {to: .to, subject: .subject}'
echo

echo "5. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.status'
echo

echo "6. Fazendo check-out (retorna comprovante)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo

echo "7. Relatório de ocupação para 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo
echo

echo "8. Relatório financeiro para 2027-03..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo
