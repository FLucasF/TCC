#!/bin/bash

set -e

echo "=== Teste de API da Pousada Maré Alta ==="
echo

PORT=${PORT:-7070}
BASE_URL="http://localhost:$PORT"

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'
echo

echo "2. Criando reserva (STANDARD, temporada regular)..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo

echo "3. Confirmando reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

echo "4. Verificando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.'
echo

echo "5. Criando reserva de alta temporada com desconto..."
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION2" | jq '.'
RESERVATION2_ID=$(echo "$RESERVATION2" | jq -r '.id')
echo

echo "6. Confirmando segunda reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/confirmation" | jq '.'
echo

echo "7. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.'
echo

echo "8. Fazendo check-out e gerando comprovante..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq '.'
echo

echo "9. Solicitando segunda via do comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq '.'
echo

echo "10. Relatório de ocupação para 10/03/2027..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo
echo

echo "11. Relatório financeiro de março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo
echo

echo "12. Relatório de taxa de turismo de julho/2027..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo
echo

echo "13. Relatório de planos de tarifa de março/2027..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-03"
echo
echo

echo "14. Cancelando reserva não reembolsável..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/cancellation" | jq '.refund'
echo

echo "=== Testes concluídos com sucesso! ==="
