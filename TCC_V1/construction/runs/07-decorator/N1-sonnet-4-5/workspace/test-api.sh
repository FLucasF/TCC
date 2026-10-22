#!/bin/bash

# Script para testar a API do sistema de reservas
# Uso: ./test-api.sh [base_url]

BASE_URL="${1:-http://localhost:7070}"

echo "=== Sistema de Reservas - Pousada Maré Alta ==="
echo "Base URL: $BASE_URL"
echo ""

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq .
echo ""

echo "2. Criando reserva (baixa temporada, sem desconto)..."
RESERVATION_1=$(curl -s -X POST "$BASE_URL/reservations" \
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
echo "$RESERVATION_1" | jq .
RESERVATION_1_ID=$(echo "$RESERVATION_1" | jq -r .id)
echo ""

echo "3. Criando reserva (alta temporada, com desconto não-reembolsável)..."
RESERVATION_2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION_2" | jq .
RESERVATION_2_ID=$(echo "$RESERVATION_2" | jq -r .id)
echo ""

echo "4. Confirmando primeira reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_1_ID/confirmation" | jq .
echo ""

echo "5. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq .
echo ""

echo "6. Fazendo check-in da primeira reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_1_ID/check-in" | jq .
echo ""

echo "7. Fazendo check-out e obtendo comprovante..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_1_ID/check-out" | jq .
echo ""

echo "8. Obtendo segunda via do comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_1_ID/document" | jq .
echo ""

echo "9. Testando conflito de reserva (mesmo quarto, mesmas datas)..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
echo ""

echo "10. Testando email inválido..."
RESERVATION_3=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Ana Paula",
    "guestEmail": "anagmail.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-03",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_3" | jq .
RESERVATION_3_ID=$(echo "$RESERVATION_3" | jq -r .id)
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_3_ID/confirmation" > /dev/null
echo "Mensagens (última deve estar BLOCKED):"
curl -s "$BASE_URL/messages" | jq '.[-1]'
echo ""

echo "11. Relatório de ocupação para 2027-03-11..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-11"
echo ""

echo "12. Relatório financeiro para março de 2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "=== Testes concluídos ==="
