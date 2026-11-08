#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'
echo

echo "2. Criando reserva..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

RESERVATION_ID=$(echo $RESERVATION | jq -r '.id')
echo $RESERVATION | jq '.'
echo

echo "3. Confirmando reserva $RESERVATION_ID..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

echo "4. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.'
echo

echo "5. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.'
echo

echo "6. Fazendo check-out..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq '.'
echo

echo "7. Consultando segunda via do comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq '.'
echo

echo "8. Criando reserva não reembolsável..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '.'
echo

echo "9. Relatório de ocupação para 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo

echo "10. Relatório financeiro de julho/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo

echo "11. Relatório de taxa de turismo de julho/2027..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo

echo "12. Relatório de planos de tarifa de março/2027..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-03"
echo

echo "=== Testes concluídos ==="
