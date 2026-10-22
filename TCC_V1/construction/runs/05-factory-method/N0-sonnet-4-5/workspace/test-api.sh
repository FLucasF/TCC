#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.'
echo

echo "2. Criando reserva com CPF..."
RESERVATION_CPF=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_CPF" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION_CPF" | jq -r '.id')
echo

echo "3. Confirmando reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

echo "4. Verificando email enviado..."
curl -s "$BASE_URL/messages" | jq '.[0]'
echo

echo "5. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.status'
echo

echo "6. Fazendo check-out e obtendo comprovante..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq '.text' -r
echo

echo "7. Criando reserva com CNPJ..."
RESERVATION_CNPJ=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Costa",
    "guestEmail": "ana@email.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Empresa ABC Ltda",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
CNPJ_ID=$(echo "$RESERVATION_CNPJ" | jq -r '.id')
curl -s -X POST "$BASE_URL/reservations/$CNPJ_ID/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/$CNPJ_ID/check-in" > /dev/null
echo "Fatura:"
curl -s -X POST "$BASE_URL/reservations/$CNPJ_ID/check-out" | jq '.text' -r
echo

echo "8. Criando reserva com Passaporte..."
RESERVATION_PASSPORT=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@email.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-20",
    "checkOut": "2027-03-22",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }')
PASSPORT_ID=$(echo "$RESERVATION_PASSPORT" | jq -r '.id')
curl -s -X POST "$BASE_URL/reservations/$PASSPORT_ID/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/$PASSPORT_ID/check-in" > /dev/null
echo "Booking Receipt:"
curl -s -X POST "$BASE_URL/reservations/$PASSPORT_ID/check-out" | jq '.text' -r
echo

echo "9. Relatório de ocupação (2027-03-10)..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo

echo "10. Relatório financeiro (março/2027)..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo

echo "=== Testes concluídos ==="
