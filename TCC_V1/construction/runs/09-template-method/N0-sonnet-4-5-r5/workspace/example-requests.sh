#!/bin/bash

# Pousada Maré Alta - Exemplos de uso da API

BASE_URL="http://localhost:7070"

echo "=== Listando quartos ==="
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n=== Criando uma reserva ==="
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')

echo -e "\n=== Consultando a reserva ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .

echo -e "\n=== Confirmando a reserva ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .

echo -e "\n=== Consultando mensagens enviadas ==="
curl -s "$BASE_URL/messages" | jq .

echo -e "\n=== Realizando check-in ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .

echo -e "\n=== Realizando check-out ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .

echo -e "\n=== Segunda via do comprovante ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .

echo -e "\n=== Relatório de ocupação ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"

echo -e "\n=== Relatório financeiro ==="
curl -s "$BASE_URL/reports/revenue?month=2027-07"

echo -e "\n=== Relatório de taxa de turismo ==="
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"

echo -e "\n=== Relatório por plano de tarifa ==="
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
