#!/bin/bash

# Script de exemplo de uso da API da Pousada Maré Alta
# Certifique-se de que o servidor está rodando antes de executar

BASE_URL="http://localhost:7070"

echo "=== Listando quartos disponíveis ==="
curl -s "$BASE_URL/rooms" | jq '.'
echo ""

echo "=== Criando reserva para pessoa física (CPF) ==="
RESERVA_CPF=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVA_CPF" | jq '.'
ID_CPF=$(echo "$RESERVA_CPF" | jq -r '.id')
echo ""

echo "=== Criando reserva para empresa (CNPJ) ==="
RESERVA_CNPJ=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVA_CNPJ" | jq '.'
ID_CNPJ=$(echo "$RESERVA_CNPJ" | jq -r '.id')
echo ""

echo "=== Criando reserva para estrangeiro (Passaporte) ==="
RESERVA_PASSPORT=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVA_PASSPORT" | jq '.'
ID_PASSPORT=$(echo "$RESERVA_PASSPORT" | jq -r '.id')
echo ""

echo "=== Confirmando reserva CPF (ID: $ID_CPF) ==="
curl -s -X POST "$BASE_URL/reservations/$ID_CPF/confirmation" | jq '.'
echo ""

echo "=== Confirmando reserva CNPJ (ID: $ID_CNPJ) ==="
curl -s -X POST "$BASE_URL/reservations/$ID_CNPJ/confirmation" | jq '.'
echo ""

echo "=== Confirmando reserva Passaporte (ID: $ID_PASSPORT) ==="
curl -s -X POST "$BASE_URL/reservations/$ID_PASSPORT/confirmation" | jq '.'
echo ""

echo "=== E-mails enviados ==="
curl -s "$BASE_URL/messages" | jq '.'
echo ""

echo "=== Fazendo check-in da reserva CPF ==="
curl -s -X POST "$BASE_URL/reservations/$ID_CPF/check-in" | jq '.'
echo ""

echo "=== Fazendo check-out da reserva CPF ==="
echo "Documento gerado (Comprovante em português):"
curl -s -X POST "$BASE_URL/reservations/$ID_CPF/check-out" | jq -r '.text'
echo ""

echo "=== Segunda via do documento CNPJ ==="
echo "Documento gerado (Fatura com ISS):"
curl -s "$BASE_URL/reservations/$ID_CNPJ/document" | jq -r '.text'
echo ""

echo "=== Segunda via do documento Passaporte ==="
echo "Documento gerado (Booking Receipt em inglês):"
curl -s "$BASE_URL/reservations/$ID_PASSPORT/document" | jq -r '.text'
echo ""

echo "=== Relatório de ocupação para 10/07/2027 ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"
echo ""

echo "=== Relatório financeiro para março/2027 ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""
