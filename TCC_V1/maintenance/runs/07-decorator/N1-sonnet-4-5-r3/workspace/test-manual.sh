#!/bin/bash

# Script de teste manual para verificar as novas funcionalidades

echo "=== Teste 1: E-mail válido em modo de produção ==="
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 101, "guestName": "Ana Silva", "guestEmail": "ana@gmail.com", "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-11", "guests": 1, "ratePlan": "STANDARD"}' | jq .

RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 102, "guestName": "Bruno Costa", "guestEmail": "bruno@example.com", "guestCpf": "222.333.444-55", "checkIn": "2027-07-10", "checkOut": "2027-07-11", "guests": 1, "ratePlan": "STANDARD"}' | jq -r .id)

echo "Confirmando reserva $RESERVATION_ID..."
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation

echo ""
echo "=== Mensagens enviadas ==="
curl -s http://localhost:7070/messages | jq '.[] | {to, subject, status}'

echo ""
echo "=== Teste 2: E-mail inválido ==="
INVALID_RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 201, "guestName": "Carlos Souza", "guestEmail": "carlos@invalid", "guestCpf": "333.444.555-66", "checkIn": "2027-07-10", "checkOut": "2027-07-11", "guests": 1, "ratePlan": "STANDARD"}' | jq -r .id)

echo "Confirmando reserva $INVALID_RESERVATION_ID..."
curl -s -X POST http://localhost:7070/reservations/$INVALID_RESERVATION_ID/confirmation

echo ""
echo "=== Mensagens (incluindo bloqueada) ==="
curl -s http://localhost:7070/messages | jq '.[] | {to, subject, status}'
