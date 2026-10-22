#!/bin/bash

API_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos disponíveis..."
curl -s "$API_URL/rooms" | python3 -m json.tool
echo
echo

echo "2. Criando reserva com CPF..."
RESERVATION_CPF=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_CPF" | python3 -m json.tool
RESERVATION_ID=$(echo "$RESERVATION_CPF" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo
echo "Reserva criada com ID: $RESERVATION_ID"
echo

echo "3. Criando reserva com CNPJ..."
RESERVATION_CNPJ=$(curl -s -X POST "$API_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_CNPJ" | python3 -m json.tool
CNPJ_ID=$(echo "$RESERVATION_CNPJ" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo

echo "4. Criando reserva com passaporte..."
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
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION_PASSPORT" | python3 -m json.tool
PASSPORT_ID=$(echo "$RESERVATION_PASSPORT" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo

echo "5. Confirmando reserva $RESERVATION_ID..."
curl -s -X POST "$API_URL/reservations/$RESERVATION_ID/confirmation" | python3 -m json.tool
echo
echo

echo "6. Verificando mensagens enviadas..."
curl -s "$API_URL/messages" | python3 -m json.tool
echo
echo

echo "7. Fazendo check-in da reserva $RESERVATION_ID..."
curl -s -X POST "$API_URL/reservations/$RESERVATION_ID/check-in" | python3 -m json.tool
echo
echo

echo "8. Fazendo check-out da reserva $RESERVATION_ID..."
DOCUMENT=$(curl -s -X POST "$API_URL/reservations/$RESERVATION_ID/check-out")
echo "$DOCUMENT" | python3 -m json.tool
echo
echo "Texto do comprovante:"
echo "$DOCUMENT" | python3 -c "import sys, json; print(json.load(sys.stdin)['text'])"
echo
echo

echo "9. Confirmando reserva CNPJ $CNPJ_ID..."
curl -s -X POST "$API_URL/reservations/$CNPJ_ID/confirmation" | python3 -m json.tool
echo
echo

echo "10. Obtendo documento da reserva CNPJ $CNPJ_ID..."
FATURA=$(curl -s "$API_URL/reservations/$CNPJ_ID/document")
echo "$FATURA" | python3 -m json.tool
echo
echo "Texto da fatura:"
echo "$FATURA" | python3 -c "import sys, json; print(json.load(sys.stdin)['text'])"
echo
echo

echo "11. Confirmando reserva passaporte $PASSPORT_ID..."
curl -s -X POST "$API_URL/reservations/$PASSPORT_ID/confirmation" | python3 -m json.tool
echo
echo

echo "12. Obtendo documento da reserva passaporte $PASSPORT_ID..."
RECEIPT=$(curl -s "$API_URL/reservations/$PASSPORT_ID/document")
echo "$RECEIPT" | python3 -m json.tool
echo
echo "Texto do booking receipt:"
echo "$RECEIPT" | python3 -c "import sys, json; print(json.load(sys.stdin)['text'])"
echo
echo

echo "13. Relatório de ocupação para 2027-03-10..."
curl -s "$API_URL/reports/occupancy?date=2027-03-10"
echo
echo

echo "14. Relatório financeiro para março de 2027..."
curl -s "$API_URL/reports/revenue?month=2027-03"
echo
echo

echo "=== Testes concluídos ==="
