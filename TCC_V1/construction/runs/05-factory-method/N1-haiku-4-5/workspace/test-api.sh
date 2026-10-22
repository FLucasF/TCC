#!/bin/bash

# Script para testar os endpoints da API

BASE_URL="http://localhost:7070"

echo "========================================"
echo "Testando API da Pousada Maré Alta"
echo "========================================"
echo ""

# 1. Listar quartos
echo "1. Listar quartos"
curl -s -X GET "$BASE_URL/rooms" | jq .
echo ""
echo ""

# 2. Criar reserva com CPF
echo "2. Criar reserva com CPF"
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Reservation ID: $RESERVATION_ID"
echo ""
echo ""

# 3. Obter detalhes da reserva
echo "3. Obter detalhes da reserva"
curl -s -X GET "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""
echo ""

# 4. Confirmar reserva
echo "4. Confirmar reserva"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo ""
echo ""

# 5. Listar mensagens enviadas
echo "5. Listar mensagens enviadas"
curl -s -X GET "$BASE_URL/messages" | jq .
echo ""
echo ""

# 6. Obter documento (segunda via)
echo "6. Obter documento (segunda via)"
curl -s -X GET "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .
echo ""
echo ""

# 7. Criar outra reserva para testar relatório
echo "7. Criar outra reserva para teste de relatório"
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION2" | jq .
RESERVATION_ID2=$(echo "$RESERVATION2" | jq -r '.id')
echo ""
echo ""

# 8. Confirmar segunda reserva
echo "8. Confirmar segunda reserva"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/confirmation" | jq .
echo ""
echo ""

# 9. Relatório de ocupação
echo "9. Relatório de ocupação para 2027-03-10"
curl -s -X GET "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""
echo ""

# 10. Relatório financeiro
echo "10. Relatório financeiro para 2027-03"
curl -s -X GET "$BASE_URL/reports/revenue?month=2027-03"
echo ""
echo ""

# 11. Check-in
echo "11. Fazer check-in na reserva $RESERVATION_ID"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo ""
echo ""

# 12. Check-out
echo "12. Fazer check-out na reserva $RESERVATION_ID"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo ""
echo ""

echo "========================================"
echo "Testes concluídos!"
echo "========================================"
