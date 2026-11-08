#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Sistema de Reservas - Pousada Maré Alta ==="
echo ""

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq .
echo ""

echo "2. Criando reserva para João Silva (quarto 101, 2 noites em março)..."
RESERVA=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo $RESERVA | jq .
RESERVA_ID=$(echo $RESERVA | jq -r .id)
echo ""

echo "3. Criando reserva para Maria Santos (quarto 102, 2 noites em julho - alta temporada)..."
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

echo "4. Criando reserva não reembolsável para Pedro Costa (quarto 201)..."
RESERVA_NR=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo $RESERVA_NR | jq .
RESERVA_NR_ID=$(echo $RESERVA_NR | jq -r .id)
echo ""

echo "5. Confirmando reserva de João Silva (ID: $RESERVA_ID)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_ID/confirmation" | jq .
echo ""

echo "6. Confirmando reserva não reembolsável (ID: $RESERVA_NR_ID)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_NR_ID/confirmation" | jq .
echo ""

echo "7. Fazendo check-in da reserva de João Silva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_ID/check-in" | jq .
echo ""

echo "8. Fazendo check-out da reserva de João Silva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVA_ID/check-out" | jq .
echo ""

echo "9. Consultando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq .
echo ""

echo "10. Gerando relatório de ocupação para 10/03/2027..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

echo "11. Gerando relatório financeiro de março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "12. Gerando relatório de taxa de turismo de março/2027..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-03"
echo ""

echo "13. Gerando relatório de planos de tarifa de março/2027..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-03"
echo ""

echo "=== Fim dos exemplos ==="
