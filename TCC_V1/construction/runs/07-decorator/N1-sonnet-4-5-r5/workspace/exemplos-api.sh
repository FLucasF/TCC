#!/bin/bash

# Script com exemplos de uso da API do sistema de reservas

BASE_URL="http://localhost:7070"

echo "=== Listar quartos ==="
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n=== Criar reserva ==="
RESERVATION_ID=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')

echo "Reserva criada com ID: $RESERVATION_ID"

echo -e "\n=== Buscar reserva ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .

echo -e "\n=== Confirmar reserva ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .

echo -e "\n=== Listar mensagens enviadas ==="
curl -s "$BASE_URL/messages" | jq .

echo -e "\n=== Check-in ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .

echo -e "\n=== Check-out ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .

echo -e "\n=== Segunda via do comprovante ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .

echo -e "\n=== Relatório de ocupação ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"

echo -e "\n=== Relatório financeiro ==="
curl -s "$BASE_URL/reports/revenue?month=2027-07"
