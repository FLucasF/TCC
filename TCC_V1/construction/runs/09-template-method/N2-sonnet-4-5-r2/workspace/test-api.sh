#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'
echo
echo

echo "2. Criando reserva STANDARD (Carla Dias)..."
RESERVATION_1=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_1" | jq '.'
echo
echo

echo "3. Criando reserva NON_REFUNDABLE em alta temporada (João Silva)..."
RESERVATION_2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION_2" | jq '.'
echo
echo

echo "4. Confirmando reserva 1..."
curl -s -X POST "$BASE_URL/reservations/1/confirmation" | jq '.'
echo
echo

echo "5. Confirmando reserva 2..."
curl -s -X POST "$BASE_URL/reservations/2/confirmation" | jq '.'
echo
echo

echo "6. Verificando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.'
echo
echo

echo "7. Fazendo check-in da reserva 1..."
curl -s -X POST "$BASE_URL/reservations/1/check-in" | jq '.'
echo
echo

echo "8. Fazendo check-out da reserva 1..."
curl -s -X POST "$BASE_URL/reservations/1/check-out" | jq '.'
echo
echo

echo "9. Consultando segunda via do comprovante..."
curl -s "$BASE_URL/reservations/1/document" | jq '.'
echo
echo

echo "10. Relatório de ocupação para 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo
echo

echo "11. Relatório financeiro de julho/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo
echo

echo "12. Relatório de taxa de turismo de julho/2027..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo
echo

echo "13. Relatório de planos de tarifa de julho/2027..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo
echo

echo "=== Testes concluídos ==="
