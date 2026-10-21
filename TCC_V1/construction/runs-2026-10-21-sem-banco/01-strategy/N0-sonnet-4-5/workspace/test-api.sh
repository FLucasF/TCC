#!/bin/bash

BASE_URL="http://localhost:7070"

echo "==== Testando API da Pousada Maré Alta ===="
echo ""

# Start server in background
echo "Iniciando servidor..."
java -jar target/reservas-1.0.0.jar &
SERVER_PID=$!
sleep 3

echo ""
echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'

echo ""
echo "2. Criando reserva com convênio TECNOSUL (DELUXE 10-12/03/2027)..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')
echo "$RESERVATION" | jq '.'
RES_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Subtotal: $(echo "$RESERVATION" | jq -r '.subtotal') (esperado: 779.80)"
echo "Desconto: $(echo "$RESERVATION" | jq -r '.discount') (esperado: 93.58)"
echo "Total: $(echo "$RESERVATION" | jq -r '.total') (esperado: 686.22)"

echo ""
echo "3. Criando reserva WEEKEND_PACKAGE (DELUXE sexta 12/03 a domingo 14/03/2027)..."
WEEKEND=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }')
echo "$WEEKEND" | jq '.'
WEEKEND_ID=$(echo "$WEEKEND" | jq -r '.id')
echo "Total: $(echo "$WEEKEND" | jq -r '.total') (esperado: 820.00)"

echo ""
echo "4. Confirmando reserva do pacote de fim de semana..."
CONFIRMED=$(curl -s -X POST "$BASE_URL/reservations/$WEEKEND_ID/confirmation")
echo "$CONFIRMED" | jq '.'

echo ""
echo "5. Cancelando reserva do pacote (deve devolver metade)..."
CANCELLED=$(curl -s -X POST "$BASE_URL/reservations/$WEEKEND_ID/cancellation")
echo "$CANCELLED" | jq '.'
echo "Reembolso: $(echo "$CANCELLED" | jq -r '.refund') (esperado: 410.00)"

echo ""
echo "6. Criando LONG_STAY (STANDARD 01-08/03/2027, 7 noites)..."
LONG_STAY=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carlos Pereira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }')
echo "$LONG_STAY" | jq '.'
echo "Subtotal: $(echo "$LONG_STAY" | jq -r '.subtotal') (esperado: 1749.30)"
echo "Desconto: $(echo "$LONG_STAY" | jq -r '.discount') (esperado: 174.93)"
echo "Total: $(echo "$LONG_STAY" | jq -r '.total') (esperado: 1574.37)"

echo ""
echo "7. Criando reserva em alta temporada (julho 2027)..."
HIGH_SEASON=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$HIGH_SEASON" | jq '.'
echo "Subtotal: $(echo "$HIGH_SEASON" | jq -r '.subtotal') (esperado: 599.76)"

echo ""
echo "8. Confirmando reserva CORPORATE..."
curl -s -X POST "$BASE_URL/reservations/$RES_ID/confirmation" > /dev/null

echo ""
echo "9. Verificando mensagens enviadas (deve ter e-mail de confirmação)..."
curl -s "$BASE_URL/messages" | jq '.[] | {to: .to, subject: .subject}'

echo ""
echo "10. Testando relatório de ocupação (10/03/2027)..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"

echo ""
echo "11. Testando relatório financeiro (março/2027)..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"

echo ""
echo "==== Testes concluídos ===="

# Kill server
kill $SERVER_PID 2>/dev/null
