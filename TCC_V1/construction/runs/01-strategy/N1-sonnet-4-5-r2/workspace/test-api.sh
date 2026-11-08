#!/bin/bash

BASE_URL="${BASE_URL:-http://localhost:7070}"

echo "=== Testando API da Pousada Maré Alta ==="
echo ""

echo "1. Listando quartos..."
curl -s "${BASE_URL}/rooms" | jq '.'
echo ""

echo "2. Criando reserva STANDARD..."
RESERVATION=$(curl -s -X POST "${BASE_URL}/reservations" \
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
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo ""

echo "3. Buscando reserva ${RESERVATION_ID}..."
curl -s "${BASE_URL}/reservations/${RESERVATION_ID}" | jq '.'
echo ""

echo "4. Confirmando reserva ${RESERVATION_ID}..."
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/confirmation" | jq '.'
echo ""

echo "5. Listando mensagens enviadas..."
curl -s "${BASE_URL}/messages" | jq '.'
echo ""

echo "6. Criando reserva CORPORATE com Tecnosul..."
CORPORATE_RES=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')
echo "$CORPORATE_RES" | jq '.'
echo ""

echo "7. Criando reserva WEEKEND_PACKAGE..."
WEEKEND_RES=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }')
echo "$WEEKEND_RES" | jq '.'
WEEKEND_ID=$(echo "$WEEKEND_RES" | jq -r '.id')
echo ""

echo "8. Criando reserva LONG_STAY..."
LONG_STAY_RES=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Lima",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }')
echo "$LONG_STAY_RES" | jq '.'
echo ""

echo "9. Fazendo check-in da reserva ${RESERVATION_ID}..."
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/check-in" | jq '.'
echo ""

echo "10. Fazendo check-out da reserva ${RESERVATION_ID}..."
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/check-out" | jq '.'
echo ""

echo "11. Obtendo documento da reserva ${RESERVATION_ID}..."
curl -s "${BASE_URL}/reservations/${RESERVATION_ID}/document" | jq '.'
echo ""

echo "12. Confirmando e cancelando reserva weekend para testar reembolso..."
curl -s -X POST "${BASE_URL}/reservations/${WEEKEND_ID}/confirmation" | jq '.'
echo ""
curl -s -X POST "${BASE_URL}/reservations/${WEEKEND_ID}/cancellation" | jq '.'
echo ""

echo "13. Relatório de ocupação para 2027-07-10..."
curl -s "${BASE_URL}/reports/occupancy?date=2027-07-10"
echo ""

echo "14. Relatório financeiro para 2027-03..."
curl -s "${BASE_URL}/reports/revenue?month=2027-03"
echo ""

echo "=== Testes concluídos ==="
