#!/bin/bash

# Script de teste da API da Pousada Maré Alta
# Requer: curl, jq (opcional para formatação JSON)

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos..."
curl -s "$BASE_URL/rooms" | jq . 2>/dev/null || curl -s "$BASE_URL/rooms"
echo
echo

echo "2. Criando reserva normal (baixa temporada)..."
RESERVATION_ID=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq -r '.id' 2>/dev/null)
echo "Reserva criada: ID $RESERVATION_ID"
echo

echo "3. Consultando reserva..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq . 2>/dev/null || curl -s "$BASE_URL/reservations/$RESERVATION_ID"
echo
echo

echo "4. Criando reserva alta temporada (julho)..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carlos Dias",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq . 2>/dev/null || curl -s -X POST "$BASE_URL/reservations" -H "Content-Type: application/json" -d '{"roomNumber":"201","guestName":"Carlos Dias","guestEmail":"carlos@example.com","guestCpf":"11122233344","checkIn":"2027-07-10","checkOut":"2027-07-12","guests":2,"ratePlan":"STANDARD"}'
echo
echo

echo "5. Criando reserva não reembolsável (com desconto)..."
RESERVATION_NR=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Maria Costa",
    "guestEmail": "maria@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq -r '.id' 2>/dev/null)
echo "Reserva não reembolsável criada: ID $RESERVATION_NR"
echo

echo "6. Confirmando primeira reserva (envia e-mail)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq . 2>/dev/null || curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation"
echo
echo

echo "7. Listando mensagens enviadas..."
curl -s "$BASE_URL/messages" | jq . 2>/dev/null || curl -s "$BASE_URL/messages"
echo
echo

echo "8. Testando e-mail inválido (será bloqueado)..."
INVALID_EMAIL_ID=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "João Santos",
    "guestEmail": "joao.gmail.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-03",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq -r '.id' 2>/dev/null)
echo "Reserva com e-mail inválido: ID $INVALID_EMAIL_ID"

echo "Confirmando (mensagem será bloqueada)..."
curl -s -X POST "$BASE_URL/reservations/$INVALID_EMAIL_ID/confirmation" > /dev/null
echo

echo "9. Verificando mensagem bloqueada..."
curl -s "$BASE_URL/messages" | jq '.[] | select(.status == "BLOCKED")' 2>/dev/null || echo "Verifique manualmente: curl $BASE_URL/messages"
echo
echo

echo "10. Fazendo check-in..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in" | jq . 2>/dev/null || curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-in"
echo
echo

echo "11. Fazendo check-out (retorna comprovante)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out" | jq . 2>/dev/null || curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/check-out"
echo
echo

echo "12. Segunda via do comprovante..."
curl -s "$BASE_URL/reservations/$RESERVATION_ID/document" | jq . 2>/dev/null || curl -s "$BASE_URL/reservations/$RESERVATION_ID/document"
echo
echo

echo "13. Relatório de ocupação (2027-03-10)..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo
echo

echo "14. Relatório financeiro (março/2027)..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo
echo

echo "15. Testando erro: quarto já reservado..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Pedro Lima",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq . 2>/dev/null || curl -s -X POST "$BASE_URL/reservations" -H "Content-Type: application/json" -d '{"roomNumber":"101","guestName":"Pedro Lima","guestEmail":"pedro@example.com","guestCpf":"11111111111","checkIn":"2027-03-10","checkOut":"2027-03-12","guests":1,"ratePlan":"STANDARD"}'
echo
echo

echo "16. Testando erro: capacidade excedida..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Grupo Grande",
    "guestEmail": "grupo@example.com",
    "guestCpf": "22222222222",
    "checkIn": "2027-05-01",
    "checkOut": "2027-05-02",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq . 2>/dev/null || curl -s -X POST "$BASE_URL/reservations" -H "Content-Type: application/json" -d '{"roomNumber":"102","guestName":"Grupo Grande","guestEmail":"grupo@example.com","guestCpf":"22222222222","checkIn":"2027-05-01","checkOut":"2027-05-02","guests":5,"ratePlan":"STANDARD"}'
echo
echo

echo "=== Testes concluídos ==="
