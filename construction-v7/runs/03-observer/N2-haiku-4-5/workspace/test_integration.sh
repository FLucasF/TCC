#!/bin/bash

# Inicia o servidor
PORT=8080 java -jar target/reserva-system-1.0.0.jar > /tmp/server.log 2>&1 &
SERVER_PID=$!
sleep 2

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "1. Listar quartos"
echo "=========================================="
curl -s -X GET "$BASE_URL/rooms" | jq .

echo ""
echo "=========================================="
echo "2. Criar reserva em alta temporada (julho) com desconto NON_REFUNDABLE"
echo "=========================================="
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESPONSE" | jq .
RES1_ID=$(echo "$RESPONSE" | jq .id)

echo ""
echo "=========================================="
echo "3. Confirmar reserva (deve enviar 3 emails)"
echo "=========================================="
curl -s -X POST "$BASE_URL/reservations/$RES1_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

echo ""
echo "=========================================="
echo "4. Criar segunda reserva (fora de alta temporada)"
echo "=========================================="
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "ana@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-04-15",
    "checkOut": "2027-04-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | jq .
RES2_ID=$(echo "$RESPONSE" | jq .id)

echo ""
echo "=========================================="
echo "5. Confirmar segunda reserva"
echo "=========================================="
curl -s -X POST "$BASE_URL/reservations/$RES2_ID/confirmation" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

echo ""
echo "=========================================="
echo "6. Check-in na primeira reserva"
echo "=========================================="
curl -s -X POST "$BASE_URL/reservations/$RES1_ID/check-in" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

echo ""
echo "=========================================="
echo "7. Check-out na primeira reserva"
echo "=========================================="
curl -s -X POST "$BASE_URL/reservations/$RES1_ID/check-out" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

echo ""
echo "=========================================="
echo "8. Cancelar segunda reserva (STANDARD - com reembolso)"
echo "=========================================="
curl -s -X POST "$BASE_URL/reservations/$RES2_ID/cancellation" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

echo ""
echo "=========================================="
echo "9. Listar mensagens enviadas"
echo "=========================================="
curl -s -X GET "$BASE_URL/messages" | jq 'length'

echo ""
echo "=========================================="
echo "10. Relatório de ocupação para 10/07/2027"
echo "=========================================="
curl -s -X GET "$BASE_URL/reports/occupancy?date=2027-07-10" | head -20

echo ""
echo "=========================================="
echo "11. Relatório financeiro para julho/2027"
echo "=========================================="
curl -s -X GET "$BASE_URL/reports/revenue?month=2027-07" | head -20

echo ""
echo "=========================================="
echo "12. Tentar criar reserva em quarto já ocupado"
echo "=========================================="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-11",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .

echo ""
echo "=========================================="
echo "13. Obter documento de segunda via"
echo "=========================================="
curl -s -X GET "$BASE_URL/reservations/$RES1_ID/document" | jq .

# Limpar
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null
