#!/bin/bash

# Script de teste básico da API
BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'
echo

echo "2. Criando reserva com CPF..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.'
echo

echo "3. Confirmando reserva..."
curl -s -X POST "$BASE_URL/reservations/1/confirmation" | jq '.'
echo

echo "4. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.'
echo

echo "5. Relatório de ocupação..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo

echo "=== Testes concluídos ==="
