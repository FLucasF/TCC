#!/bin/bash

# Script para testar a API do sistema de reservas
# Certifique-se de que o sistema está rodando antes de executar este script

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq
echo

echo "2. Criando reserva STANDARD..."
RESERVATION_1=$(curl -s -X POST "$BASE_URL/reservations" \
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
  }' | jq)
echo "$RESERVATION_1"
RESERVATION_1_ID=$(echo "$RESERVATION_1" | jq -r '.id')
echo

echo "3. Criando reserva NON_REFUNDABLE..."
RESERVATION_2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-13",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq)
echo "$RESERVATION_2"
RESERVATION_2_ID=$(echo "$RESERVATION_2" | jq -r '.id')
echo

echo "4. Confirmando reserva 1..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_1_ID/confirmation" | jq
echo

echo "5. Confirmando reserva 2..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_2_ID/confirmation" | jq
echo

echo "6. Fazendo check-in da reserva 1..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_1_ID/check-in" | jq
echo

echo "7. Fazendo check-out da reserva 1..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_1_ID/check-out" | jq
echo

echo "8. Consultando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq
echo

echo "9. Relatório de ocupação para 2027-07-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo
echo

echo "10. Relatório financeiro para julho/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo
echo

echo "11. Relatório de taxa de turismo para julho/2027..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo
echo

echo "12. Relatório de planos de tarifa para julho/2027..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo
echo

echo "=== Testes concluídos ==="
