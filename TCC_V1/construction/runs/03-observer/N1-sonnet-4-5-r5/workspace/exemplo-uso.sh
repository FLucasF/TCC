#!/bin/bash

# Script de exemplo de uso da API da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Listando quartos disponíveis ==="
curl -s "$BASE_URL/rooms" | jq .
echo ""

echo "=== Criando uma reserva ==="
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo ""

echo "=== Confirmando a reserva $RESERVATION_ID ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo ""

echo "=== Consultando mensagens enviadas (emails) ==="
curl -s "$BASE_URL/messages" | jq .
echo ""

echo "=== Consultando a reserva ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""

echo "=== Obtendo comprovante (segunda via) ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .
echo ""

echo "=== Testando reserva em alta temporada (julho) ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
echo ""

echo "=== Testando plano não reembolsável ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
echo ""

echo "=== Relatório de ocupação para 2027-03-10 ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

echo "=== Relatório financeiro para março/2027 ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""
