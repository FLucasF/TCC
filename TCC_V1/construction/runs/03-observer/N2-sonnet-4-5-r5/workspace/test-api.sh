#!/bin/bash
# Script de teste da API do sistema de reservas

BASE_URL="http://localhost:7070"

echo "=== Teste do Sistema de Reservas - Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'
echo

echo "2. Criando reserva para o quarto 201..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Reserva criada com ID: $RESERVATION_ID"
echo

echo "3. Confirmando reserva (isso vai gerar 3 e-mails)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

echo "4. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.[] | {to: .to, subject: .subject}'
echo

echo "5. Criando reserva com desconto NON_REFUNDABLE..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '{id, subtotal, discount, total, ratePlan}'
echo

echo "6. Tentando criar reserva conflitante (deve dar erro)..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Souza",
    "guestEmail": "pedro@example.com",
    "guestCpf": "99999999999",
    "checkIn": "2027-03-11",
    "checkOut": "2027-03-13",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.'
echo

echo "7. Gerando relatório de ocupação para 10/03/2027..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo

echo "8. Gerando relatório financeiro de março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo

echo "=== Testes concluídos ==="
