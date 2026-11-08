#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Listando quartos ==="
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n=== Criando reserva ==="
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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

echo "$RESERVATION" | jq .

RESERVATION_ID=$(echo "$RESERVATION" | jq -r .id)
echo "ID da reserva: $RESERVATION_ID"

echo -e "\n=== Confirmando reserva ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .

echo -e "\n=== Listando mensagens enviadas ==="
curl -s "$BASE_URL/messages" | jq .

echo -e "\n=== Obtendo comprovante ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .

echo -e "\n=== Criando reserva não reembolsável ==="
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }')

echo "$RESERVATION2" | jq .

RESERVATION_ID2=$(echo "$RESERVATION2" | jq -r .id)

echo -e "\n=== Confirmando segunda reserva ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/confirmation" | jq .

echo -e "\n=== Cancelando reserva não reembolsável ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/cancellation" | jq .

echo -e "\n=== Relatório de ocupação para 10/03/2027 ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"

echo -e "\n=== Relatório financeiro para março de 2027 ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"
