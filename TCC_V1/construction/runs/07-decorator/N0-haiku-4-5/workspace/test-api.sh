#!/bin/bash

BASE_URL="http://localhost:7070"

echo "==============================================="
echo "Teste da API de Reservas - Pousada Maré Alta"
echo "==============================================="

# Teste 1: GET /rooms
echo -e "\n1. Listando quartos disponíveis..."
curl -s "$BASE_URL/rooms" | jq '.' | head -30

# Teste 2: POST /reservations - Criar reserva STANDARD
echo -e "\n\n2. Criando reserva STANDARD..."
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESPONSE" | jq '.'
RESERVATION_ID=$(echo "$RESPONSE" | jq '.id')

# Teste 3: POST /reservations/{id}/confirmation
echo -e "\n3. Confirmando reserva #$RESERVATION_ID..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'

# Teste 4: GET /messages - Verificar emails (deve ter 2: um para hóspede, um cópia arquivo)
echo -e "\n4. Verificando mensagens de email enviadas..."
curl -s "$BASE_URL/messages" | jq '.'

# Teste 5: POST /reservations/{id}/check-in
echo -e "\n5. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.'

# Teste 6: POST /reservations/{id}/check-out
echo -e "\n6. Fazendo check-out (gera comprovante)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out"
echo ""

# Teste 7: GET /reservations/{id}/document
echo -e "\n7. Solicitando segunda via do comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document"
echo ""

# Teste 8: Criar reserva NON_REFUNDABLE
echo -e "\n\n8. Criando reserva NON_REFUNDABLE com desconto..."
RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESPONSE" | jq '.'
RESERVATION_ID2=$(echo "$RESPONSE" | jq '.id')

# Teste 9: GET /reports/occupancy
echo -e "\n9. Relatório de ocupação para 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

# Teste 10: GET /reports/revenue
echo -e "\n10. Relatório de receita para 2027-07..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo ""

# Teste 11: Email inválido
echo -e "\n11. Tentando criar reserva com email inválido..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Test User",
    "guestEmail": "invalid@gmail",
    "guestCpf": "11111111111",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.'

curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Test User",
    "guestEmail": "valid@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.id' > /tmp/res3.txt

RES3=$(cat /tmp/res3.txt)

echo -e "\n12. Confirmando reserva #$RES3 para testar email inválido..."
curl -s -X POST "$BASE_URL/reservations/$RES3/confirmation" | jq '.status'

# Teste 12: Mensagens bloqueadas
echo -e "\n13. Verificando mensagens (deve ter email bloqueado)..."
curl -s "$BASE_URL/messages" | jq '.[] | select(.status == "BLOCKED")'

echo -e "\n==============================================="
echo "Testes completos!"
echo "==============================================="
