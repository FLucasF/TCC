#!/bin/bash

BASE_URL=${1:-http://localhost:7070}

echo "========================================="
echo "Exemplos de uso da API - Pousada Maré Alta"
echo "========================================="
echo ""

echo "1. Listar quartos disponíveis:"
echo "GET $BASE_URL/rooms"
curl -s "$BASE_URL/rooms" | jq .
echo ""
echo ""

echo "2. Criar reserva com CPF (pessoa física):"
echo "POST $BASE_URL/reservations"
RESERVATION_CPF=$(curl -s -X POST "$BASE_URL/reservations" \
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
echo "$RESERVATION_CPF" | jq .
RESERVATION_ID=$(echo "$RESERVATION_CPF" | jq -r .id)
echo ""
echo ""

echo "3. Criar reserva com CNPJ (empresa):"
echo "POST $BASE_URL/reservations"
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-04-15",
    "checkOut": "2027-04-17",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
echo ""
echo ""

echo "4. Criar reserva com passaporte (estrangeiro):"
echo "POST $BASE_URL/reservations"
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }' | jq .
echo ""
echo ""

echo "5. Consultar reserva:"
echo "GET $BASE_URL/reservations/$RESERVATION_ID"
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .
echo ""
echo ""

echo "6. Confirmar reserva (pagar):"
echo "POST $BASE_URL/reservations/$RESERVATION_ID/confirmation"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo ""
echo ""

echo "7. Listar mensagens (e-mails enviados):"
echo "GET $BASE_URL/messages"
curl -s "$BASE_URL/messages" | jq '.[-1]'
echo ""
echo ""

echo "8. Fazer check-in:"
echo "POST $BASE_URL/reservations/$RESERVATION_ID/check-in"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq .
echo ""
echo ""

echo "9. Fazer check-out e obter comprovante:"
echo "POST $BASE_URL/reservations/$RESERVATION_ID/check-out"
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq .
echo ""
echo ""

echo "10. Obter segunda via do documento:"
echo "GET $BASE_URL/reservations/$RESERVATION_ID/document"
DOCUMENT=$(curl -s "$BASE_URL/reservations/$RESERVATION_ID/document")
echo "$DOCUMENT" | jq .
echo ""
echo "Texto do documento:"
echo "$DOCUMENT" | jq -r .text
echo ""
echo ""

echo "11. Relatório de ocupação:"
echo "GET $BASE_URL/reports/occupancy?date=2027-03-10"
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""
echo ""

echo "12. Relatório financeiro:"
echo "GET $BASE_URL/reports/revenue?month=2027-03"
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""
echo ""

echo "========================================="
echo "Exemplos concluídos!"
echo "========================================="
