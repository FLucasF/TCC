#!/bin/bash

# Script de demonstração do sistema de reservas
# Inicia o servidor, cria algumas reservas de teste e mostra os relatórios

set -e

JAR="target/reservas-1.0.0.jar"

if [ ! -f "$JAR" ]; then
  echo "JAR não encontrado. Execute: mvn package"
  exit 1
fi

echo "🏨 Iniciando servidor de reservas..."
java -jar "$JAR" > /tmp/pousada.log 2>&1 &
SERVER_PID=$!
sleep 2

trap "kill $SERVER_PID 2>/dev/null; wait 2>/dev/null" EXIT

echo ""
echo "✅ Servidor iniciado (PID: $SERVER_PID)"
echo ""

# Criar reservas de teste
echo "📝 Criando reservas de teste..."
echo ""

# Reserva 1: STANDARD em fevereiro (high season)
R1=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-02-10",
    "checkOut": "2027-02-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
ID1=$(echo $R1 | jq -r '.id')
echo "✓ Reserva #$ID1: João Silva (Quarto 101, STANDARD, 2 noites em fevereiro)"
echo "  Valor: R$ $(echo $R1 | jq -r '.total | @json' | tr -d '"') (high season: +20%)"

# Reserva 2: NON_REFUNDABLE em julho
R2=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-18",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
ID2=$(echo $R2 | jq -r '.id')
echo "✓ Reserva #$ID2: Maria Santos (Quarto 201, NON_REFUNDABLE, 3 noites em julho)"
echo "  Subtotal: R$ $(echo $R2 | jq -r '.subtotal')"
echo "  Desconto (15%): R$ $(echo $R2 | jq -r '.discount')"
echo "  Total: R$ $(echo $R2 | jq -r '.total')"

# Reserva 3: DELUXE em março (low season)
R3=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Pedro Oliveira",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55555555555",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-03",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID3=$(echo $R3 | jq -r '.id')
echo "✓ Reserva #$ID3: Pedro Oliveira (Quarto 202, STANDARD, 2 noites em março)"

echo ""
echo "💳 Confirmando reservas..."

curl -s -X POST http://localhost:7070/reservations/$ID1/confirmation > /dev/null
curl -s -X POST http://localhost:7070/reservations/$ID2/confirmation > /dev/null
curl -s -X POST http://localhost:7070/reservations/$ID3/confirmation > /dev/null

echo "✓ Todas as reservas confirmadas"
echo ""

# Fazer check-in em uma
echo "🚪 Realizando check-in na reserva #$ID1..."
curl -s -X POST http://localhost:7070/reservations/$ID1/check-in > /dev/null
echo "✓ Check-in realizado"
echo ""

# Fazer check-out em uma
echo "🔑 Realizando check-out na reserva #$ID1..."
CHECKOUT=$(curl -s -X POST http://localhost:7070/reservations/$ID1/check-out)
echo "✓ Check-out realizado"
echo ""

# Exibir comprovante
echo "📄 Comprovante de saída:"
echo "$CHECKOUT" | jq '.title, .guestName, .roomNumber, .checkIn, .checkOut, .nights, .total' | tr -d '"'
echo ""

# Mensagens enviadas
echo "📧 Emails enviados:"
MESSAGES=$(curl -s http://localhost:7070/messages)
echo "$MESSAGES" | jq -r '.[] | "  → \(.subject) para \(.to)"' | head -10
echo ""

# Relatório de ocupação
echo "📊 Relatório de Ocupação (15/07/2027):"
curl -s "http://localhost:7070/reports/occupancy?date=2027-07-15"
echo ""

echo "💰 Relatório Financeiro (2027-07):"
curl -s "http://localhost:7070/reports/revenue?month=2027-07"

echo ""
echo "✅ Demonstração concluída!"
