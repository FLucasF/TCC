#!/bin/bash

API_URL="http://localhost:7070"

echo "=== Pousada Maré Alta - Testes da API ==="
echo ""

echo "1. Listando quartos..."
curl -s "$API_URL/rooms" | python3 -m json.tool
echo ""

echo "2. Criando reserva com CPF..."
RESERVA_CPF=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVA_CPF" | python3 -m json.tool
RESERVA_ID=$(echo "$RESERVA_CPF" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo "ID da reserva: $RESERVA_ID"
echo ""

echo "3. Confirmando reserva..."
curl -s -X POST "$API_URL/reservations/$RESERVA_ID/confirmation" | python3 -m json.tool
echo ""

echo "4. Consultando mensagens enviadas..."
curl -s "$API_URL/messages" | python3 -m json.tool
echo ""

echo "5. Criando reserva com CNPJ..."
RESERVA_CNPJ=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@tecnosul.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVA_CNPJ" | python3 -m json.tool
RESERVA_ID2=$(echo "$RESERVA_CNPJ" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo ""

echo "6. Confirmando segunda reserva..."
curl -s -X POST "$API_URL/reservations/$RESERVA_ID2/confirmation" | python3 -m json.tool
echo ""

echo "7. Criando reserva com Passaporte..."
RESERVA_PASS=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVA_PASS" | python3 -m json.tool
RESERVA_ID3=$(echo "$RESERVA_PASS" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo ""

echo "8. Confirmando terceira reserva..."
curl -s -X POST "$API_URL/reservations/$RESERVA_ID3/confirmation" | python3 -m json.tool
echo ""

echo "9. Relatório de ocupação para 2027-03-10..."
curl -s "$API_URL/reports/occupancy?date=2027-03-10"
echo ""

echo "10. Relatório financeiro para março/2027..."
curl -s "$API_URL/reports/revenue?month=2027-03"
echo ""

echo "11. Fazendo check-in da primeira reserva..."
curl -s -X POST "$API_URL/reservations/$RESERVA_ID/check-in" | python3 -m json.tool
echo ""

echo "12. Fazendo check-out e obtendo comprovante..."
curl -s -X POST "$API_URL/reservations/$RESERVA_ID/check-out" | python3 -m json.tool
echo ""

echo "=== Testes concluídos ==="
