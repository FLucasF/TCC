#!/bin/bash

# Script de teste da API - Pousada Maré Alta
BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq .
echo

echo "2. Criando reserva STANDARD..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
echo

echo "3. Criando reserva CORPORATE (Tecnosul)..."
RESERVA_ID=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }' | jq -r .id)
echo "Reserva criada: ID $RESERVA_ID"
echo

echo "4. Criando reserva WEEKEND_PACKAGE..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@email.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }' | jq .
echo

echo "5. Criando reserva LONG_STAY..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@email.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }' | jq .
echo

echo "6. Confirmando reserva $RESERVA_ID..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_ID/confirmation" | jq .
echo

echo "7. Consultando reserva $RESERVA_ID..."
curl -s "$BASE_URL/reservations/$RESERVA_ID" | jq .
echo

echo "8. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq .
echo

echo "9. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_ID/check-in" | jq .
echo

echo "10. Fazendo check-out e obtendo comprovante..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_ID/check-out" | jq .
echo

echo "11. Obtendo segunda via do comprovante..."
curl -s "$BASE_URL/reservations/$RESERVA_ID/document" | jq .
echo

echo "12. Relatório de ocupação (2027-03-10)..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo
echo

echo "13. Relatório financeiro (março/2027)..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo

echo "=== Testes concluídos ==="
