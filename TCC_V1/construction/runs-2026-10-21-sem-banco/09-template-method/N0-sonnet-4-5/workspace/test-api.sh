#!/bin/bash
set -e

BASE_URL="http://localhost:7070"

echo "=== Testando API da Pousada Maré Alta ==="

# Iniciar servidor em background
java -jar target/reservas-1.0.0.jar > /dev/null 2>&1 &
SERVER_PID=$!
sleep 3

# Garantir que o servidor será parado ao final
trap "kill $SERVER_PID 2>/dev/null || true" EXIT

echo "✓ Servidor iniciado (PID: $SERVER_PID)"

# 1. Listar quartos
echo -n "1. GET /rooms... "
ROOMS=$(curl -s "$BASE_URL/rooms")
if echo "$ROOMS" | grep -q "101"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 2. Criar reserva
echo -n "2. POST /reservations... "
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
    -H "Content-Type: application/json" \
    -d '{
        "roomNumber": "101",
        "guestName": "João Silva",
        "guestEmail": "joao@email.com",
        "guestCpf": "12345678900",
        "checkIn": "2027-07-10",
        "checkOut": "2027-07-12",
        "guests": 2,
        "ratePlan": "STANDARD"
    }')
if echo "$RESERVATION" | grep -q "PENDING"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 3. Confirmar reserva
echo -n "3. POST /reservations/1/confirmation... "
CONFIRMED=$(curl -s -X POST "$BASE_URL/reservations/1/confirmation")
if echo "$CONFIRMED" | grep -q "CONFIRMED"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 4. Verificar mensagens
echo -n "4. GET /messages... "
MESSAGES=$(curl -s "$BASE_URL/messages")
if echo "$MESSAGES" | grep -q "Reserva confirmada"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 5. Relatório de ocupação
echo -n "5. GET /reports/occupancy?date=2027-07-10... "
OCCUPANCY=$(curl -s "$BASE_URL/reports/occupancy?date=2027-07-10")
if echo "$OCCUPANCY" | grep -q "total,1/5"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 6. Relatório financeiro
echo -n "6. GET /reports/revenue?month=2027-07... "
REVENUE=$(curl -s "$BASE_URL/reports/revenue?month=2027-07")
if echo "$REVENUE" | grep -q "599.76"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 7. Relatório de taxa de turismo
echo -n "7. GET /reports/tourism-tax?month=2027-07... "
TAX=$(curl -s "$BASE_URL/reports/tourism-tax?month=2027-07")
if echo "$TAX" | grep -q "18.00"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

# 8. Relatório de planos de tarifa
echo -n "8. GET /reports/rate-plans?month=2027-07... "
PLANS=$(curl -s "$BASE_URL/reports/rate-plans?month=2027-07")
if echo "$PLANS" | grep -q "STANDARD,1"; then
    echo "✓"
else
    echo "✗ Falhou"
    exit 1
fi

echo ""
echo "=== Todos os testes passaram! ==="
