#!/bin/bash

API_URL="http://localhost:7070"

echo "=== Teste da API Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$API_URL/rooms" | jq '.'
echo

echo "2. Criando reserva com CPF..."
RESERVATION_ID=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')
echo "Reserva criada: $RESERVATION_ID"
echo

echo "3. Consultando reserva..."
curl -s "$API_URL/reservations/$RESERVATION_ID" | jq '.'
echo

echo "4. Confirmando reserva..."
curl -s -X POST "$API_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

echo "5. Listando mensagens enviadas..."
curl -s "$API_URL/messages" | jq '.'
echo

echo "6. Obtendo documento..."
curl -s "$API_URL/reservations/$RESERVATION_ID/document" | jq '.'
echo

echo "7. Criando reserva com CNPJ..."
RESERVATION_CNPJ=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')
echo "Reserva CNPJ criada: $RESERVATION_CNPJ"
echo

echo "8. Criando reserva com passaporte..."
RESERVATION_PASSPORT=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq -r '.id')
echo "Reserva passaporte criada: $RESERVATION_PASSPORT"
echo

echo "9. Relatório de ocupação para 2027-07-10..."
curl -s "$API_URL/reports/occupancy?date=2027-07-10"
echo

echo "10. Relatório financeiro para 2027-07..."
curl -s "$API_URL/reports/revenue?month=2027-07"
echo
