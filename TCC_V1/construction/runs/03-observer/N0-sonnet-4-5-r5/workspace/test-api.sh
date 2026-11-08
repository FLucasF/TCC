#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos disponíveis..."
curl -s "$BASE_URL/rooms" | jq .
echo
echo

echo "2. Criando uma reserva..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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
echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r .id)
echo
echo "Reserva ID: $RESERVATION_ID"
echo

echo "3. Confirmando a reserva (isso vai enviar notificações)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo
echo

echo "4. Verificando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq .
echo
echo

echo "5. Consultando a reserva..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo
echo

echo "6. Obtendo comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq .
echo
echo

echo "7. Criando uma reserva não reembolsável em alta temporada..."
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
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
echo "$RESERVATION2" | jq .
RESERVATION_ID2=$(echo "$RESERVATION2" | jq -r .id)
echo
echo

echo "8. Confirmando segunda reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/confirmation" | jq .
echo
echo

echo "9. Cancelando a segunda reserva (não reembolsável, então financeiro não é notificado)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID2/cancellation" | jq .
echo
echo

echo "10. Verificando todas as mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.[] | {id, to, subject}'
echo
echo

echo "11. Testando relatório de ocupação para 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo
echo

echo "12. Testando relatório financeiro para março de 2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo
echo

echo "=== Testes concluídos ==="
