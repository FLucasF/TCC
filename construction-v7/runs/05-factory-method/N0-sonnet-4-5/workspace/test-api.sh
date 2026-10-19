#!/bin/bash

# Script de teste da API da Pousada Maré Alta

PORT=7070
BASE_URL="http://localhost:$PORT"

echo "=== Testando API da Pousada Maré Alta ==="
echo

# Testar GET /rooms
echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq '.' || echo "ERRO"
echo

# Criar reserva com CPF
echo "2. Criando reserva com CPF..."
RESERVATION_CPF=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_CPF" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION_CPF" | jq -r '.id')
echo

# Confirmar reserva
echo "3. Confirmando reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

# Verificar mensagens
echo "4. Verificando e-mails enviados..."
curl -s "$BASE_URL/messages" | jq '.'
echo

# Criar reserva com CNPJ
echo "5. Criando reserva com CNPJ..."
RESERVATION_CNPJ=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_CNPJ" | jq '.'
RESERVATION_ID_2=$(echo "$RESERVATION_CNPJ" | jq -r '.id')
echo

# Confirmar segunda reserva
echo "6. Confirmando segunda reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID_2/confirmation" | jq '.'
echo

# Criar reserva com passaporte
echo "7. Criando reserva com passaporte..."
RESERVATION_PASSPORT=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-16",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION_PASSPORT" | jq '.'
RESERVATION_ID_3=$(echo "$RESERVATION_PASSPORT" | jq -r '.id')
echo

# Check-in
echo "8. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq '.'
echo

# Check-out
echo "9. Fazendo check-out..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq '.'
echo

# Obter documento (segunda via)
echo "10. Obtendo segunda via do documento..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID_2/document" | jq '.'
echo

# Relatório de ocupação
echo "11. Relatório de ocupação para 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo

# Relatório financeiro
echo "12. Relatório financeiro para março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo

# Relatório financeiro para julho/2027
echo "13. Relatório financeiro para julho/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-07"
echo

echo "=== Testes concluídos ==="
