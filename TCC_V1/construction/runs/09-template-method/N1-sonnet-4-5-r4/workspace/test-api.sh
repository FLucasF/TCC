#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos disponíveis..."
curl -s "$BASE_URL/rooms" | jq '.'
echo

echo "2. Criando uma reserva STANDARD..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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

RESERVATION_ID=$(echo $RESERVATION | jq -r '.id')
echo "Reserva criada: ID $RESERVATION_ID"
echo $RESERVATION | jq '.'
echo

echo "3. Confirmando a reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

echo "4. Buscando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.[-1]'
echo

echo "5. Obtendo comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq '.'
echo

echo "6. Criando reserva NON_REFUNDABLE em alta temporada..."
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }')

RESERVATION2_ID=$(echo $RESERVATION2 | jq -r '.id')
echo "Reserva criada: ID $RESERVATION2_ID"
echo $RESERVATION2 | jq '{id, nights, subtotal, discount, total}'
echo

echo "7. Confirmando segunda reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/confirmation" | jq '{id, status, amountPaid}'
echo

echo "8. Gerando relatório de ocupação..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo

echo "9. Gerando relatório financeiro..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo

echo "10. Gerando relatório de taxa de turismo..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo

echo "11. Gerando relatório de planos de tarifa..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo

echo "=== Testes concluídos ==="
