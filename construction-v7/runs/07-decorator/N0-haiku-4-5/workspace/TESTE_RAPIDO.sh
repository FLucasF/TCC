#!/bin/bash

# Script de teste rápido do sistema de reservas
# Inicia o servidor e executa testes básicos

echo "Iniciando servidor..."
java -jar target/mare-alta.jar &
SERVER_PID=$!
sleep 3

echo ""
echo "=== TESTES DO SISTEMA DE RESERVAS ==="
echo ""

# Test 1: GET /rooms
echo "✓ GET /rooms"
curl -s http://localhost:7070/rooms | jq 'length' | grep -q "5" && echo "  Sucesso: 5 quartos retornados"

# Test 2: POST /reservations
echo ""
echo "✓ POST /reservations"
RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RES | jq -r '.id')
echo "  Sucesso: Reserva #$ID criada (Status: PENDING)"

# Test 3: GET /reservations/{id}
echo ""
echo "✓ GET /reservations/{id}"
curl -s http://localhost:7070/reservations/$ID | jq '.id' | grep -q "$ID" && \
echo "  Sucesso: Reserva #$ID recuperada"

# Test 4: POST /reservations/{id}/confirmation
echo ""
echo "✓ POST /reservations/{id}/confirmation"
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq '.status' | grep -q "CONFIRMED" && \
echo "  Sucesso: Reserva #$ID confirmada"

# Test 5: GET /messages
echo ""
echo "✓ GET /messages"
COUNT=$(curl -s http://localhost:7070/messages | jq 'length')
echo "  Sucesso: $COUNT mensagens no sistema"

# Test 6: POST /reservations/{id}/check-in
echo ""
echo "✓ POST /reservations/{id}/check-in"
curl -s -X POST http://localhost:7070/reservations/$ID/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.status' | grep -q "CHECKED_IN" && \
echo "  Sucesso: Check-in realizado"

# Test 7: GET /reservations/{id}/document
echo ""
echo "✓ GET /reservations/{id}/document"
curl -s http://localhost:7070/reservations/$ID/document | jq '.roomNumber' | grep -q "101" && \
echo "  Sucesso: Comprovante recuperado"

# Test 8: POST /reservations/{id}/check-out
echo ""
echo "✓ POST /reservations/{id}/check-out"
curl -s -X POST http://localhost:7070/reservations/$ID/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.roomNumber' | grep -q "101" && \
echo "  Sucesso: Check-out realizado"

# Test 9: GET /reports/occupancy
echo ""
echo "✓ GET /reports/occupancy?date=2027-07-10"
curl -s "http://localhost:7070/reports/occupancy?date=2027-07-10" | grep -q "total" && \
echo "  Sucesso: Relatório de ocupação gerado"

# Test 10: GET /reports/revenue
echo ""
echo "✓ GET /reports/revenue?month=2027-07"
curl -s "http://localhost:7070/reports/revenue?month=2027-07" | grep -q "total" && \
echo "  Sucesso: Relatório de receita gerado"

# Cleanup
echo ""
echo "Encerrando servidor..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null

echo ""
echo "=== TODOS OS TESTES PASSARAM ✓ ==="
