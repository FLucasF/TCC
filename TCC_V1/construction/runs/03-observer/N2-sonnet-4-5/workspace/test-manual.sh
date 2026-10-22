#!/bin/bash

echo "=== Sistema de Reservas - Pousada Maré Alta ==="
echo "Teste manual da API REST"
echo ""

BASE_URL="http://localhost:7070"

echo "1. Consultando quartos..."
curl -s "$BASE_URL/rooms" | jq '.' || echo "Erro ao consultar quartos"
echo ""

echo "2. Criando uma reserva normal em temporada regular..."
RESERVA_1=$(curl -s -X POST "$BASE_URL/reservations" \
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
echo "$RESERVA_1" | jq '.'
echo ""

echo "3. Criando uma reserva em alta temporada..."
RESERVA_2=$(curl -s -X POST "$BASE_URL/reservations" \
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
  }')
echo "$RESERVA_2" | jq '.'
echo ""

echo "4. Criando uma reserva não reembolsável..."
RESERVA_3=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVA_3" | jq '.'
echo ""

echo "5. Confirmando a reserva 1 (deve gerar 3 notificações)..."
curl -s -X POST "$BASE_URL/reservations/1/confirmation" | jq '.'
echo ""

echo "6. Verificando as mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.[] | {to: .to, subject: .subject}'
echo ""

echo "7. Obtendo comprovante da reserva 1..."
curl -s "$BASE_URL/reservations/1/document" | jq '.'
echo ""

echo "8. Confirmando e cancelando a reserva 2 (deve gerar notificações de cancelamento)..."
curl -s -X POST "$BASE_URL/reservations/2/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/2/cancellation" | jq '.'
echo ""

echo "9. Confirmando e cancelando a reserva 3 não reembolsável (sem notificação de reembolso)..."
curl -s -X POST "$BASE_URL/reservations/3/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/3/cancellation" | jq '.'
echo ""

echo "10. Criando mais uma reserva para relatório de ocupação..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Paulo Costa",
    "guestEmail": "paulo@example.com",
    "guestCpf": "33333333333",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }' > /dev/null
curl -s -X POST "$BASE_URL/reservations/4/confirmation" > /dev/null
echo ""

echo "11. Relatório de ocupação para 2027-05-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-05-10"
echo ""

echo "12. Relatório financeiro para março de 2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "13. Total de mensagens enviadas..."
TOTAL=$(curl -s "$BASE_URL/messages" | jq 'length')
echo "Total: $TOTAL mensagens"
echo ""

echo "=== Teste concluído ==="
