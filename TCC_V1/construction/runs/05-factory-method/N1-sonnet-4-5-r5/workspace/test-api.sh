#!/bin/bash

# Script de teste da API da Pousada Maré Alta

BASE_URL="${BASE_URL:-http://localhost:7070}"

echo "=== Testando API da Pousada Maré Alta ==="
echo "Base URL: $BASE_URL"
echo ""

# 1. Listar quartos
echo "1. Listando quartos disponíveis..."
curl -s "$BASE_URL/rooms" | jq '.'
echo ""

# 2. Criar reserva com CPF
echo "2. Criando reserva com CPF..."
RESERVATION_CPF=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_CPF" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION_CPF" | jq -r '.id')
echo ""

# 3. Confirmar reserva
echo "3. Confirmando reserva $RESERVATION_ID..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo ""

# 4. Listar mensagens enviadas
echo "4. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq '.'
echo ""

# 5. Criar reserva com CNPJ
echo "5. Criando reserva corporativa..."
RESERVATION_COMPANY=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@tecnosul.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_COMPANY" | jq '.'
COMPANY_ID=$(echo "$RESERVATION_COMPANY" | jq -r '.id')
echo ""

# 6. Confirmar e obter documento corporativo
echo "6. Confirmando reserva corporativa..."
curl -s -X POST "$BASE_URL/reservations/$COMPANY_ID/confirmation" | jq '.'
echo ""

# 7. Criar reserva com passaporte
echo "7. Criando reserva com passaporte..."
RESERVATION_PASSPORT=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-20",
    "checkOut": "2027-03-22",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION_PASSPORT" | jq '.'
PASSPORT_ID=$(echo "$RESERVATION_PASSPORT" | jq -r '.id')
echo ""

# 8. Confirmar reserva com passaporte
echo "8. Confirmando reserva com passaporte..."
curl -s -X POST "$BASE_URL/reservations/$PASSPORT_ID/confirmation" | jq '.'
echo ""

# 9. Fazer check-in
echo "9. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.'
echo ""

# 10. Fazer check-out e obter comprovante
echo "10. Fazendo check-out e obtendo comprovante..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq '.'
echo ""

# 11. Obter documento (segunda via)
echo "11. Obtendo segunda via do documento..."
curl -s "$BASE_URL/reservations/$COMPANY_ID/document" | jq '.'
echo ""

# 12. Relatório de ocupação
echo "12. Relatório de ocupação para 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

# 13. Relatório financeiro
echo "13. Relatório financeiro de março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

echo "=== Testes concluídos ==="
