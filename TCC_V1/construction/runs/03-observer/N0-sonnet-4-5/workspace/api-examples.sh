#!/bin/bash

# Exemplos de uso da API do sistema de reservas
# Execute o servidor antes de testar estes comandos

BASE_URL="http://localhost:7070"

echo "=== QUARTOS ==="
echo "Listar quartos disponíveis:"
curl -s "$BASE_URL/rooms" | jq .
echo ""

echo "=== CRIAR RESERVA ==="
echo "Criar reserva em quarto STANDARD (baixa temporada):"
RESERVATION_ID=$(curl -s -X POST "$BASE_URL/reservations" \
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
  }' | jq -r '.id')

echo "Reserva criada com ID: $RESERVATION_ID"
echo ""

echo "=== CONFIRMAR RESERVA ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo ""

echo "=== MENSAGENS ENVIADAS ==="
echo "Ver emails enviados:"
curl -s "$BASE_URL/messages" | jq .
echo ""

echo "=== RELATÓRIO DE OCUPAÇÃO ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

echo "=== CRIAR RESERVA COM DESCONTO ==="
echo "Criar reserva NON_REFUNDABLE em alta temporada:"
RESERVATION2_ID=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq -r '.id')

echo "Reserva criada com ID: $RESERVATION2_ID"
echo ""

curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/confirmation" | jq .
echo ""

echo "=== CHECK-IN ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo ""

echo "=== CHECK-OUT ==="
echo "Comprovante gerado no check-out:"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo ""

echo "=== SEGUNDA VIA DO COMPROVANTE ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .
echo ""

echo "=== CANCELAMENTO ==="
echo "Cancelar reserva NON_REFUNDABLE (sem reembolso):"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/cancellation" | jq .
echo ""

echo "=== RELATÓRIO FINANCEIRO ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "=== MENSAGENS APÓS CANCELAMENTO ==="
curl -s "$BASE_URL/messages" | jq 'length'
echo "emails enviados no total"
