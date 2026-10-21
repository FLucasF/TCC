#!/bin/bash

# Script para testar a API da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo ""

echo "1. Listando quartos disponíveis:"
curl -s "$BASE_URL/rooms" | json_pp || curl -s "$BASE_URL/rooms"
echo ""
echo ""

echo "2. Criando reserva com CPF:"
RESERVATION_1=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_1" | json_pp 2>/dev/null || echo "$RESERVATION_1"
echo ""
echo ""

echo "3. Criando reserva com CNPJ (empresa):"
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyName": "Tecnosul Ltda",
    "companyCnpj": "12.345.678/0001-90",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | json_pp 2>/dev/null || curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyName": "Tecnosul Ltda",
    "companyCnpj": "12.345.678/0001-90",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
echo ""
echo ""

echo "4. Criando reserva com Passaporte (estrangeiro) e desconto:"
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | json_pp 2>/dev/null || curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
echo ""
echo ""

echo "5. Confirmando reserva 1:"
curl -s -X POST "$BASE_URL/reservations/1/confirmation" | json_pp 2>/dev/null || curl -s -X POST "$BASE_URL/reservations/1/confirmation"
echo ""
echo ""

echo "6. Fazendo check-in da reserva 1:"
curl -s -X POST "$BASE_URL/reservations/1/check-in" | json_pp 2>/dev/null || curl -s -X POST "$BASE_URL/reservations/1/check-in"
echo ""
echo ""

echo "7. Fazendo check-out da reserva 1 (retorna documento):"
curl -s -X POST "$BASE_URL/reservations/1/check-out" | json_pp 2>/dev/null || curl -s -X POST "$BASE_URL/reservations/1/check-out"
echo ""
echo ""

echo "8. Listando mensagens enviadas:"
curl -s "$BASE_URL/messages" | json_pp 2>/dev/null || curl -s "$BASE_URL/messages"
echo ""
echo ""

echo "9. Relatório de ocupação para 2027-03-10:"
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""
echo ""

echo "10. Relatório financeiro de março/2027:"
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""
echo ""

echo "11. Testando preço em alta temporada (julho):"
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | json_pp 2>/dev/null || curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
echo ""
echo ""

echo "=== Testes concluídos ==="
