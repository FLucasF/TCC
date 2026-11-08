#!/bin/bash

# Script de teste da API da Pousada Maré Alta
# Ajuste a URL base conforme necessário

BASE_URL="http://localhost:7070"

echo "=== Teste da API da Pousada Maré Alta ==="
echo ""

# 1. Listar quartos
echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.' || echo "Erro ao listar quartos"
echo ""

# 2. Criar uma reserva
echo "2. Criando uma reserva..."
RESERVATION_RESPONSE=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION_RESPONSE" | jq '.'

# Extrair o ID da reserva (assume que a resposta tem um campo "id")
RESERVATION_ID=$(echo "$RESERVATION_RESPONSE" | jq -r '.id' 2>/dev/null)
echo "Reserva criada com ID: $RESERVATION_ID"
echo ""

if [ ! -z "$RESERVATION_ID" ] && [ "$RESERVATION_ID" != "null" ]; then
    # 3. Buscar a reserva
    echo "3. Buscando a reserva..."
    curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq '.'
    echo ""

    # 4. Confirmar a reserva
    echo "4. Confirmando a reserva..."
    curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" \
      -H "Content-Type: application/json" \
      -d '{"amountPaid": 599.76}' | jq '.'
    echo ""

    # 5. Listar mensagens (e-mail de confirmação)
    echo "5. Listando mensagens enviadas..."
    curl -s "$BASE_URL/messages" | jq '.'
    echo ""

    # 6. Fazer check-in
    echo "6. Realizando check-in..."
    curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" \
      -H "Content-Type: application/json" \
      -d '{}' | jq '.'
    echo ""

    # 7. Fazer check-out
    echo "7. Realizando check-out..."
    curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" \
      -H "Content-Type: application/json" \
      -d '{}' | jq '.'
    echo ""

    # 8. Obter segunda via do comprovante
    echo "8. Obtendo segunda via do comprovante..."
    curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq '.'
    echo ""
fi

# 9. Relatório de ocupação
echo "9. Relatório de ocupação (2027-07-10)..."
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo ""

# 10. Relatório financeiro
echo "10. Relatório financeiro (2027-07)..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo ""

# 11. Relatório de taxa de turismo
echo "11. Relatório de taxa de turismo (2027-07)..."
curl -s "$BASE_URL/reports/tourism-tax?month=2027-07"
echo ""

# 12. Relatório de planos de tarifa
echo "12. Relatório de planos de tarifa (2027-07)..."
curl -s "$BASE_URL/reports/rate-plans?month=2027-07"
echo ""

echo "=== Fim dos testes ==="
