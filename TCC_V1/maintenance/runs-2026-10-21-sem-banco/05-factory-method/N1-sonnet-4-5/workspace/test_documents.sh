#!/bin/bash

# Script para testar os três tipos de documentos

echo "Iniciando testes manuais dos documentos..."

# Iniciar o servidor em background
java -jar target/hotel.jar &
SERVER_PID=$!
sleep 3

BASE_URL="http://localhost:7070"

echo ""
echo "=== Teste 1: Reserva com CPF (pessoa física) ==="
RESERVATION1=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-06-01",
    "checkOut": "2027-06-03",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID1=$(echo $RESERVATION1 | grep -o '"id":[0-9]*' | cut -d':' -f2)
curl -s -X POST "$BASE_URL/reservations/$ID1/confirmation"
curl -s -X POST "$BASE_URL/reservations/$ID1/check-in"
DOCUMENT1=$(curl -s -X POST "$BASE_URL/reservations/$ID1/check-out")
echo "$DOCUMENT1" | grep -o '"text":"[^"]*"' | sed 's/\\n/\n/g' | sed 's/"text":"//;s/"$//'

echo ""
echo "=== Teste 2: Reserva com CNPJ (empresa) ==="
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "João Santos",
    "guestEmail": "joao@example.com",
    "companyCnpj": "98.765.432/0001-10",
    "companyName": "Tech Solutions Ltda",
    "checkIn": "2027-06-01",
    "checkOut": "2027-06-03",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }')
ID2=$(echo $RESERVATION2 | grep -o '"id":[0-9]*' | cut -d':' -f2)
curl -s -X POST "$BASE_URL/reservations/$ID2/confirmation"
curl -s -X POST "$BASE_URL/reservations/$ID2/check-in"
DOCUMENT2=$(curl -s -X POST "$BASE_URL/reservations/$ID2/check-out")
echo "$DOCUMENT2" | grep -o '"text":"[^"]*"' | sed 's/\\n/\n/g' | sed 's/"text":"//;s/"$//'

echo ""
echo "=== Teste 3: Reserva com passaporte (estrangeiro) ==="
RESERVATION3=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 103,
    "guestName": "Robert Johnson",
    "guestEmail": "robert@example.com",
    "guestPassport": "AB123456",
    "guestCountry": "Canada",
    "checkIn": "2027-06-01",
    "checkOut": "2027-06-02",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
ID3=$(echo $RESERVATION3 | grep -o '"id":[0-9]*' | cut -d':' -f2)
curl -s -X POST "$BASE_URL/reservations/$ID3/confirmation"
curl -s -X POST "$BASE_URL/reservations/$ID3/check-in"
DOCUMENT3=$(curl -s -X POST "$BASE_URL/reservations/$ID3/check-out")
echo "$DOCUMENT3" | grep -o '"text":"[^"]*"' | sed 's/\\n/\n/g' | sed 's/"text":"//;s/"$//'

echo ""
echo "=== Parando servidor ==="
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo ""
echo "Testes concluídos!"
