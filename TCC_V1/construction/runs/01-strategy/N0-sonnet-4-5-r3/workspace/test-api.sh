#!/bin/bash

# Script de teste da API da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="
echo

echo "1. Listando quartos disponíveis..."
curl -s "${BASE_URL}/rooms" | jq '.'
echo
echo

echo "2. Criando reserva STANDARD..."
RESERVATION=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

RESERVATION_ID=$(echo $RESERVATION | jq -r '.id')
echo "Reserva criada: ID $RESERVATION_ID"
echo $RESERVATION | jq '.'
echo
echo

echo "3. Confirmando reserva..."
curl -s -X POST "${BASE_URL}/reservations/${RESERVATION_ID}/confirmation" | jq '.'
echo
echo

echo "4. Criando reserva CORPORATE (Tecnosul)..."
CORP_RESERVATION=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')

CORP_ID=$(echo $CORP_RESERVATION | jq -r '.id')
echo "Reserva corporativa criada: ID $CORP_ID"
echo $CORP_RESERVATION | jq '.'
echo
echo

echo "5. Confirmando reserva corporativa..."
curl -s -X POST "${BASE_URL}/reservations/${CORP_ID}/confirmation" | jq '.'
echo
echo

echo "6. Criando reserva WEEKEND_PACKAGE..."
WEEKEND=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Carlos Oliveira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }')

WEEKEND_ID=$(echo $WEEKEND | jq -r '.id')
echo "Reserva de fim de semana criada: ID $WEEKEND_ID"
echo $WEEKEND | jq '.'
echo
echo

echo "7. Confirmando e cancelando reserva de fim de semana..."
curl -s -X POST "${BASE_URL}/reservations/${WEEKEND_ID}/confirmation" | jq '.'
echo
curl -s -X POST "${BASE_URL}/reservations/${WEEKEND_ID}/cancellation" | jq '.'
echo
echo

echo "8. Criando reserva LONG_STAY..."
LONG=$(curl -s -X POST "${BASE_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Paula",
    "guestEmail": "ana@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }')

LONG_ID=$(echo $LONG | jq -r '.id')
echo "Reserva de estadia longa criada: ID $LONG_ID"
echo $LONG | jq '.'
echo
echo

echo "9. Listando mensagens enviadas..."
curl -s "${BASE_URL}/messages" | jq '.'
echo
echo

echo "10. Obtendo comprovante da reserva ${CORP_ID}..."
curl -s "${BASE_URL}/reservations/${CORP_ID}/document" | jq '.'
echo
echo

echo "11. Relatório de ocupação para 10/07/2027..."
curl -s "${BASE_URL}/reports/occupancy?date=2027-07-10"
echo
echo

echo "12. Relatório financeiro para março/2027..."
curl -s "${BASE_URL}/reports/revenue?month=2027-03"
echo

echo
echo "=== Testes concluídos ==="
