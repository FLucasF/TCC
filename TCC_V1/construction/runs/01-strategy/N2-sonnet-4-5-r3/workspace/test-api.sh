#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq .
echo

echo "2. Criando reserva STANDARD..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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
  }')
echo $RESERVATION | jq .
RESERVATION_ID=$(echo $RESERVATION | jq -r '.id')
echo

echo "3. Confirmando reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo

echo "4. Verificando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq .
echo

echo "5. Criando reserva CORPORATE (Tecnosul)..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }' | jq .
echo

echo "6. Criando reserva WEEKEND_PACKAGE..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Santos",
    "guestEmail": "pedro@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }' | jq .
echo

echo "7. Criando reserva LONG_STAY..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }' | jq .
echo

echo "8. Relatório de ocupação para 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo
echo

echo "9. Relatório financeiro para 2027-07..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo
